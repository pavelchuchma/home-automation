package org.chuma.homecontroller.extensions.metrics;

import org.chuma.homecontroller.extensions.external.AbstractStateMonitor;
import org.chuma.homecontroller.extensions.external.boiler.BoilerManager;
import org.chuma.homecontroller.extensions.external.boiler.State;

/**
 * {@code boiler}: one point per successful gateway read, stamped with the read time. The monitor refreshes
 * every 10 minutes (a refresh walks the boiler display), so a tick with no new read writes nothing.
 */
public class BoilerMetrics implements MetricSource {
    private final BoilerManager manager;
    private long lastWrittenTimestamp;

    public BoilerMetrics(BoilerManager manager) {
        this.manager = manager;
    }

    @Override
    public void sample(LineProtocolWriter writer, long nowMs) {
        AbstractStateMonitor.Snapshot<State> snapshot = manager.getBoilerMonitor().peekState();
        if (snapshot == null || snapshot.timestamp == lastWrittenTimestamp) {
            return;
        }
        lastWrittenTimestamp = snapshot.timestamp;
        State s = snapshot.value;
        writer.line("boiler", snapshot.timestamp)
                .field("mode", s.getDisplayMode())
                .field("status_age_s", s.getStatusAge())
                .field("t5u_c", s.getTempT5U())
                .field("t5l_c", s.getTempT5L())
                .field("t3_c", s.getTempT3())
                .field("t4_c", s.getTempT4())
                .field("tp_c", s.getTempTP())
                .field("th_c", s.getTempTh())
                .field("target_c", s.getTargetTemp())
                .field("on", s.isOn())
                .field("hot", s.isHot())
                .field("eheat", s.isEHeat())
                .field("pump", s.isPump())
                .field("vacation", s.isVacation())
                .field("last_fail", s.getLastFailure() == null ? null : s.getLastFailure().toString())
                .field("scheduled_on", manager.isScheduledOn())
                .field("disinfect_req", manager.isDisinfect())
                .field("eheat_req", manager.isEHeat())
                .field("target_req_c", manager.getTargetTemp())
                .end();
    }
}
