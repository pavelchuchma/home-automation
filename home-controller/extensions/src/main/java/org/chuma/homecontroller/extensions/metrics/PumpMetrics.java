package org.chuma.homecontroller.extensions.metrics;

import java.util.List;

import org.chuma.homecontroller.extensions.actor.WaterPumpMonitor;

/**
 * {@code pump}: an event on every on/off edge. {@code pump-min}: per tick, the number of cycles that ended
 * since the previous tick and their total run time; a cycle counts in the minute it ended.
 */
public class PumpMetrics implements MetricSource {
    private final WaterPumpMonitor monitor;
    private long lastTickMs;

    public PumpMetrics(WaterPumpMonitor monitor, MetricsReporter reporter) {
        this.monitor = monitor;
        lastTickMs = System.currentTimeMillis();
        monitor.addListener((on, timestampMs) -> reporter.event(w -> w.line("pump", timestampMs).field("on", on).end()));
    }

    @Override
    public void sample(LineProtocolWriter writer, long nowMs) {
        List<WaterPumpMonitor.Record> ended = monitor.getRecordsEndedAfter(lastTickMs);
        lastTickMs = nowMs;
        double runSeconds = 0;
        for (WaterPumpMonitor.Record r : ended) {
            runSeconds += r.duration;
        }
        writer.line("pump-min", nowMs)
                .field("cycles", ended.size())
                .field("run_s", runSeconds)
                .end();
    }
}
