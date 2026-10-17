package org.chuma.homecontroller.extensions.metrics;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import org.chuma.homecontroller.extensions.external.watertank.WaterTankMonitor;

public class TankMetricsTest {
    private static class RecordingSender implements MetricsReporter.Sender {
        final List<String> bodies = new ArrayList<>();

        @Override
        public void send(String body) {
            bodies.add(body);
        }
    }

    @Test
    public void readingsBecomeEventsFillOnlyForGoodOnes() {
        RecordingSender sender = new RecordingSender();
        MetricsReporter reporter = new MetricsReporter("home", sender);
        WaterTankMonitor tank = new WaterTankMonitor(450, 2150, 15 * 60_000);
        new TankMetrics(tank, reporter);

        WaterTankMonitor.Reading ok = tank.newReading(1300, "OK", 49, 25, -67, "Deep-Sleep Wake");
        tank.recordReading(ok);
        WaterTankMonitor.Reading fail = tank.newReading(0, "FAIL", 49, 3, -70, "Power On");
        tank.recordReading(fail);
        reporter.tick(ok.receivedAtMs + 60_000);

        Assert.assertEquals(1, sender.bodies.size());
        String body = sender.bodies.get(0);
        Assert.assertTrue(body, body.contains("home-tank status=\"OK\",distance_mm=1300i,samples=49i,valid_samples=25i,rssi=-67i,reset_reason=\"Deep-Sleep Wake\",fill_pct=50i " + ok.receivedAtMs + "\n"));
        Assert.assertTrue(body, body.contains("home-tank status=\"FAIL\",distance_mm=0i,samples=49i,valid_samples=3i,rssi=-70i,reset_reason=\"Power On\" " + fail.receivedAtMs + "\n"));
    }
}
