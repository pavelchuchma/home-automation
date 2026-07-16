package org.chuma.homecontroller.extensions.external.watertank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.extensions.external.AbstractStateMonitor;

/**
 * Polls a water-level-meter ESP for the rainwater tank fill level.
 * <p>
 * The ESP can report {@code status: FAIL} (electrical interference on the sensor wiring) or fail
 * to respond at all (see {@link WaterTankClient}). In either case the last successfully computed
 * fill percentage is kept for {@code validityWindowMs} before {@link #getFillPercent()} falls back
 * to {@code -1} (unknown).
 */
public class WaterTankMonitor extends AbstractStateMonitor<Integer> {
    private static final Logger log = LoggerFactory.getLogger(WaterTankMonitor.class.getName());

    private final WaterTankClient client;
    private final int fullDistanceMm;
    private final int emptyDistanceMm;
    private final long validityWindowMs;
    private int lastGoodPercent = -1;
    private long lastGoodTimestampMs = 0;

    /**
     * @param host            hostname/IP of the ESP (e.g. {@code sonic.local})
     * @param fullDistanceMm  sensor-to-water distance in mm when the tank is full
     * @param emptyDistanceMm sensor-to-water distance in mm when the tank is empty
     */
    public WaterTankMonitor(String host, int fullDistanceMm, int emptyDistanceMm,
                             int refreshIntervalMs, int maxUnusedRunTimeMs, long validityWindowMs) {
        super("WaterTankMonitor", refreshIntervalMs, maxUnusedRunTimeMs);
        this.client = new WaterTankClient(host);
        this.fullDistanceMm = fullDistanceMm;
        this.emptyDistanceMm = emptyDistanceMm;
        this.validityWindowMs = validityWindowMs;
    }

    @Override
    protected Integer getStateImpl(boolean firstCallAfterSleep) {
        try {
            WaterTankClient.Reading r = client.readLevel();
            if ("OK".equals(r.status)) {
                lastGoodPercent = computePercent(r.distanceMm);
                lastGoodTimestampMs = System.currentTimeMillis();
                log.debug("Water tank level: {} mm -> {}%", r.distanceMm, lastGoodPercent);
            } else {
                log.warn("Water tank status FAIL (ageSec={}, samples={}, validSamples={})", r.ageSec, r.samples, r.validSamples);
            }
        } catch (Exception e) {
            log.error("Failed to read water tank level", e);
        }

        boolean valid = System.currentTimeMillis() - lastGoodTimestampMs <= validityWindowMs;
        return valid ? lastGoodPercent : -1;
    }

    private int computePercent(int distanceMm) {
        int percent = Math.round(100f * (emptyDistanceMm - distanceMm) / (emptyDistanceMm - fullDistanceMm));
        return Math.max(0, Math.min(100, percent));
    }

    /**
     * @return fill percentage 0-100, or -1 if there is no valid reading within the validity window
     * (or the monitor isn't running, e.g. no host configured)
     */
    public int getFillPercent() {
        if (!isRunning()) {
            return -1;
        }
        Integer percent = getState();
        return percent == null ? -1 : percent;
    }
}
