package org.chuma.homecontroller.extensions.metrics;

import org.chuma.homecontroller.extensions.external.AbstractStateMonitor;
import org.chuma.homecontroller.extensions.external.robonect.RobonectMonitor;
import org.chuma.homecontroller.extensions.external.robonect.State;
import org.chuma.homecontroller.extensions.external.robonect.client.model.Gps;
import org.chuma.homecontroller.extensions.external.robonect.client.model.Status;

/**
 * {@code mower}: the Robonect state. Off season the mower is powered down, the monitor backs off and this
 * writes nothing.
 */
public class MowerMetrics implements MetricSource {
    private final RobonectMonitor monitor;

    public MowerMetrics(RobonectMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void sample(LineProtocolWriter writer, long nowMs) {
        AbstractStateMonitor.Snapshot<State> snapshot = monitor.peekState();
        if (snapshot == null) {
            return;
        }
        State s = snapshot.value;
        LineProtocolWriter.Line line = writer.line("mower", nowMs);
        Status status = s.status();
        if (status != null) {
            line.field("status", status.getStatus())
                    .field("mode", status.getMode())
                    .field("battery_pct", status.getBattery())
                    .field("duration_s", status.getDuration())
                    .field("hours", status.getHours())
                    .field("distance_m", status.getDistance())
                    .field("home", status.isHome())
                    .field("stopped", status.isStopped());
        }
        if (s.wlan() != null) {
            line.field("wlan_dbm", s.wlan().getSignal());
        }
        if (s.health() != null) {
            line.field("temp_c", s.health().getTemperature()).field("humidity_pct", s.health().getHumidity());
        }
        if (s.error() != null && s.error().getErrorCode() != null) {
            line.field("error_code", s.error().getErrorCode());
        }
        Gps gps = s.gps();
        if (gps != null && gps.getLatitude() != 0 && gps.getLongitude() != 0) {
            line.field("lat", gps.getLatitude()).field("lon", gps.getLongitude());
        }
        line.end();
    }
}
