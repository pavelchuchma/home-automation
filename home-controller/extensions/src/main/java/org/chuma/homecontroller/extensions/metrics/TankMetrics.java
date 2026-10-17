package org.chuma.homecontroller.extensions.metrics;

import org.chuma.homecontroller.extensions.external.watertank.WaterTankMonitor;

/**
 * {@code tank}: one event per reading pushed by the water-level-meter ESP (~5 min). {@code fill_pct} only
 * for good readings; a FAIL reading still records the sensor diagnostics.
 */
public class TankMetrics {
    public TankMetrics(WaterTankMonitor monitor, MetricsReporter reporter) {
        monitor.addListener(reading -> reporter.event(w -> {
            LineProtocolWriter.Line line = w.line("tank", reading.receivedAtMs)
                    .field("status", reading.status)
                    .field("distance_mm", reading.distanceMm)
                    .field("samples", reading.samples)
                    .field("valid_samples", reading.validSamples)
                    .field("rssi", reading.rssi)
                    .field("reset_reason", reading.resetReason);
            if (WaterTankMonitor.STATUS_OK.equals(reading.status)) {
                line.field("fill_pct", monitor.getFillPercent());
            }
            line.end();
        }));
    }
}
