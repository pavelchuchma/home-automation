package org.chuma.homecontroller.extensions.metrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class MetricsReporterTest {
    private static class RecordingSender implements MetricsReporter.Sender {
        final List<String> bodies = new ArrayList<>();
        boolean fail;

        @Override
        public void send(String body) throws IOException {
            if (fail) {
                throw new IOException("HTTP 500: boom");
            }
            bodies.add(body);
        }
    }

    @Test
    public void tickSamplesSourcesAndAppendsEventsInOneBody() {
        RecordingSender sender = new RecordingSender();
        MetricsReporter reporter = new MetricsReporter("home", sender);
        reporter.addSource((w, now) -> w.line("a", now).field("v", 1).end());
        reporter.addSource((w, now) -> w.line("b", now).field("v", 2).end());
        reporter.event(w -> w.line("ev", 500).field("on", true).end());

        reporter.tick(60_000);

        Assert.assertEquals(1, sender.bodies.size());
        Assert.assertEquals("home-a v=1i 60000\nhome-b v=2i 60000\nhome-ev on=true 500\n", sender.bodies.get(0));

        // events are flushed once
        reporter.tick(120_000);
        Assert.assertEquals("home-a v=1i 120000\nhome-b v=2i 120000\n", sender.bodies.get(1));
    }

    @Test
    public void emptyTickSendsNothing() {
        RecordingSender sender = new RecordingSender();
        MetricsReporter reporter = new MetricsReporter("home", sender);
        reporter.addSource((w, now) -> { /* device offline, nothing to write */ });
        reporter.event(w -> { /* no fields */ });
        reporter.tick(60_000);
        Assert.assertTrue(sender.bodies.isEmpty());
    }

    @Test
    public void failingSourceDoesNotBreakTheOthers() {
        RecordingSender sender = new RecordingSender();
        MetricsReporter reporter = new MetricsReporter("home", sender);
        reporter.addSource((w, now) -> { throw new IllegalStateException("broken"); });
        reporter.addSource((w, now) -> w.line("b", now).field("v", 2).end());
        reporter.tick(60_000);
        Assert.assertEquals("home-b v=2i 60000\n", sender.bodies.get(0));
    }

    @Test
    public void failedSendDropsTheBatchAndContinues() {
        RecordingSender sender = new RecordingSender();
        MetricsReporter reporter = new MetricsReporter("home", sender);
        reporter.addSource((w, now) -> w.line("a", now).field("v", 1).end());
        sender.fail = true;
        reporter.tick(60_000);
        reporter.tick(120_000);
        sender.fail = false;
        reporter.tick(180_000);
        Assert.assertEquals(1, sender.bodies.size());
        Assert.assertEquals("home-a v=1i 180000\n", sender.bodies.get(0));
    }
}
