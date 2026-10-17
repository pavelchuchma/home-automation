package org.chuma.homecontroller.extensions.metrics;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import org.chuma.homecontroller.extensions.actor.WaterPumpMonitor;

public class PumpMetricsTest {
    private static class RecordingSender implements MetricsReporter.Sender {
        final List<String> bodies = new ArrayList<>();

        @Override
        public void send(String body) {
            bodies.add(body);
        }
    }

    @Test
    public void edgesBecomeEventsAndFinishedCyclesAreCountedPerTick() throws InterruptedException {
        RecordingSender sender = new RecordingSender();
        MetricsReporter reporter = new MetricsReporter("home", sender);
        WaterPumpMonitor pump = new WaterPumpMonitor();
        reporter.addSource(new PumpMetrics(pump, reporter));

        pump.getOnAction().perform(0);
        Thread.sleep(20);
        // a 1.5 s cycle ending now
        pump.getOffAction().perform(1_500);
        long now = System.currentTimeMillis() + 1;
        reporter.tick(now);

        Assert.assertEquals(1, sender.bodies.size());
        String body = sender.bodies.get(0);
        Assert.assertTrue(body, body.startsWith("home-pump-min cycles=1i,run_s=1.5 " + now + "\n"));
        Assert.assertTrue(body, body.contains("home-pump on=true "));
        Assert.assertTrue(body, body.contains("home-pump on=false "));

        // nothing ended since the last tick
        reporter.tick(now + 60_000);
        Assert.assertEquals("home-pump-min cycles=0i,run_s=0.0 " + (now + 60_000) + "\n", sender.bodies.get(1));
    }

    @Test
    public void recordsEndedAfterReturnsOnlyCyclesThatEndedLater() throws InterruptedException {
        WaterPumpMonitor pump = new WaterPumpMonitor();
        pump.getOffAction().perform(1_000);
        long between = System.currentTimeMillis() + 1;
        Thread.sleep(5);
        pump.getOffAction().perform(2_000);

        Assert.assertEquals(2, pump.getRecordsEndedAfter(0).size());
        List<WaterPumpMonitor.Record> later = pump.getRecordsEndedAfter(between);
        Assert.assertEquals(1, later.size());
        Assert.assertEquals(2.0, later.get(0).duration, 0.0001);
        Assert.assertTrue(pump.getRecordsEndedAfter(System.currentTimeMillis() + 1_000).isEmpty());
    }
}
