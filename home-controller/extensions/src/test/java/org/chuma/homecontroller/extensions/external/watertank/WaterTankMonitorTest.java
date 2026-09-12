package org.chuma.homecontroller.extensions.external.watertank;

import org.junit.Assert;
import org.junit.Test;

public class WaterTankMonitorTest {
    private static final int FULL_MM = 450;
    private static final int EMPTY_MM = 2150;
    private static final long WINDOW_MS = 15 * 60_000;

    private long now = 1_000_000_000L;
    private final WaterTankMonitor monitor = new WaterTankMonitor(FULL_MM, EMPTY_MM, WINDOW_MS, () -> now);

    private void push(int distanceMm, String status) {
        monitor.recordReading(monitor.newReading(distanceMm, status, 49, 25, -67, "Deep-Sleep Wake"));
    }

    @Test
    public void unknownBeforeFirstPush() {
        Assert.assertEquals(-1, monitor.getFillPercent());
        Assert.assertNull(monitor.getLastReading());
        Assert.assertEquals(-1, monitor.getLastReadingAgeMs());
    }

    @Test
    public void computesPercentFromDistance() {
        push(FULL_MM, "OK");
        Assert.assertEquals(100, monitor.getFillPercent());
        push(EMPTY_MM, "OK");
        Assert.assertEquals(0, monitor.getFillPercent());
        push((FULL_MM + EMPTY_MM) / 2, "OK");
        Assert.assertEquals(50, monitor.getFillPercent());
    }

    @Test
    public void clampsOutOfRangeDistances() {
        push(FULL_MM - 200, "OK");
        Assert.assertEquals(100, monitor.getFillPercent());
        push(EMPTY_MM + 500, "OK");
        Assert.assertEquals(0, monitor.getFillPercent());
    }

    @Test
    public void failKeepsLastGoodValueButIsReported() {
        push(FULL_MM, "OK");
        now += 60_000;
        push(0, "FAIL");
        Assert.assertEquals(100, monitor.getFillPercent());
        Assert.assertEquals("FAIL", monitor.getLastReading().status);
        Assert.assertEquals(0, monitor.getLastReadingAgeMs());
    }

    @Test
    public void goodValueExpiresAfterValidityWindow() {
        push(FULL_MM, "OK");
        now += WINDOW_MS;
        Assert.assertEquals(100, monitor.getFillPercent());
        now += 1;
        Assert.assertEquals(-1, monitor.getFillPercent());
        Assert.assertEquals(WINDOW_MS + 1, monitor.getLastReadingAgeMs());
    }

    @Test
    public void failOnlyDoesNotRevive() {
        push(FULL_MM, "OK");
        now += WINDOW_MS + 1;
        push(0, "FAIL");
        Assert.assertEquals(-1, monitor.getFillPercent());
    }
}
