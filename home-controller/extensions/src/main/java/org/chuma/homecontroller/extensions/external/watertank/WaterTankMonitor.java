package org.chuma.homecontroller.extensions.external.watertank;

import java.util.function.LongSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps the rainwater tank fill level as pushed by the water-level-meter ESP.
 * <p>
 * The ESP wakes up periodically, measures the sensor-to-water distance and POSTs it to the
 * {@code /rest/wtank/push} endpoint, which calls {@link #recordReading(Reading)}. There is no
 * polling here: this class only remembers the last pushed reading. The ESP can report
 * {@code status: FAIL} (too few valid echoes, electrical interference on the sensor wiring); such
 * a reading is kept for diagnostics but does not touch the last good fill percentage.
 * {@link #getFillPercent()} falls back to {@code -1} (unknown) when the last good reading is older
 * than {@code validityWindowMs}, i.e. when several pushes in a row were missed or failed.
 */
public class WaterTankMonitor {
    public static final String STATUS_OK = "OK";

    private static final Logger log = LoggerFactory.getLogger(WaterTankMonitor.class.getName());

    private final int fullDistanceMm;
    private final int emptyDistanceMm;
    private final long validityWindowMs;
    private final LongSupplier clock;

    private Reading lastReading;
    private int lastGoodPercent = -1;
    private long lastGoodTimestampMs;
    private boolean hasGoodReading;

    /**
     * @param fullDistanceMm   sensor-to-water distance in mm when the tank is full
     * @param emptyDistanceMm  sensor-to-water distance in mm when the tank is empty
     * @param validityWindowMs how long the last good reading stays valid without a newer one
     */
    public WaterTankMonitor(int fullDistanceMm, int emptyDistanceMm, long validityWindowMs) {
        this(fullDistanceMm, emptyDistanceMm, validityWindowMs, System::currentTimeMillis);
    }

    WaterTankMonitor(int fullDistanceMm, int emptyDistanceMm, long validityWindowMs, LongSupplier clock) {
        this.fullDistanceMm = fullDistanceMm;
        this.emptyDistanceMm = emptyDistanceMm;
        this.validityWindowMs = validityWindowMs;
        this.clock = clock;
    }

    /**
     * Records a reading pushed by the ESP.
     */
    public synchronized void recordReading(Reading reading) {
        lastReading = reading;
        if (STATUS_OK.equals(reading.status)) {
            lastGoodPercent = computePercent(reading.distanceMm);
            lastGoodTimestampMs = reading.receivedAtMs;
            hasGoodReading = true;
            log.debug("Water tank level: {} mm -> {}% (samples={}, validSamples={}, rssi={}, resetReason={})",
                    reading.distanceMm, lastGoodPercent, reading.samples, reading.validSamples,
                    reading.rssi, reading.resetReason);
        } else {
            log.warn("Water tank status {} (samples={}, validSamples={}, rssi={}, resetReason={})",
                    reading.status, reading.samples, reading.validSamples, reading.rssi, reading.resetReason);
        }
    }

    private int computePercent(int distanceMm) {
        int percent = Math.round(100f * (emptyDistanceMm - distanceMm) / (emptyDistanceMm - fullDistanceMm));
        return Math.max(0, Math.min(100, percent));
    }

    /**
     * @return fill percentage 0-100, or -1 if there is no good reading within the validity window
     */
    public synchronized int getFillPercent() {
        if (!hasGoodReading || clock.getAsLong() - lastGoodTimestampMs > validityWindowMs) {
            return -1;
        }
        return lastGoodPercent;
    }

    /**
     * @return the most recently pushed reading regardless of its status, or null before the first push
     */
    public synchronized Reading getLastReading() {
        return lastReading;
    }

    /**
     * @return milliseconds since the last reading was received, or -1 before the first push
     */
    public synchronized long getLastReadingAgeMs() {
        return lastReading == null ? -1 : clock.getAsLong() - lastReading.receivedAtMs;
    }

    /**
     * Creates a reading time-stamped with the monitor's clock.
     */
    public Reading newReading(int distanceMm, String status, int samples, int validSamples, int rssi, String resetReason) {
        return new Reading(distanceMm, status, samples, validSamples, rssi, resetReason, clock.getAsLong());
    }

    /**
     * One measurement as pushed by the ESP.
     */
    public static class Reading {
        public final int distanceMm;
        public final String status;
        public final int samples;
        public final int validSamples;
        /** Wi-Fi signal strength in dBm at the time of the push. */
        public final int rssi;
        /** ESP reset reason text, e.g. "Deep-Sleep Wake" or "Power On"; may be null. */
        public final String resetReason;
        public final long receivedAtMs;

        public Reading(int distanceMm, String status, int samples, int validSamples, int rssi, String resetReason,
                       long receivedAtMs) {
            this.distanceMm = distanceMm;
            this.status = status;
            this.samples = samples;
            this.validSamples = validSamples;
            this.rssi = rssi;
            this.resetReason = resetReason;
            this.receivedAtMs = receivedAtMs;
        }
    }
}
