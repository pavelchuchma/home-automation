package org.chuma.homecontroller.extensions.metrics;

import org.chuma.homecontroller.extensions.external.AbstractStateMonitor;
import org.chuma.homecontroller.extensions.external.futura.FuturaMonitor;
import org.chuma.homecontroller.extensions.external.futura.State;

/**
 * {@code futura}: the recuperation unit. {@code temp_ambient_c} is the outdoor air temperature.
 */
public class FuturaMetrics implements MetricSource {
    private final FuturaMonitor monitor;

    public FuturaMetrics(FuturaMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void sample(LineProtocolWriter writer, long nowMs) {
        AbstractStateMonitor.Snapshot<State> snapshot = monitor.peekState();
        if (snapshot == null) {
            return;
        }
        State s = snapshot.value;
        writer.line("futura", nowMs)
                .field("speed", s.getVentilationSpeed())
                .field("temp_ambient_c", s.getAirTempAmbient())
                .field("temp_fresh_c", s.getAirTempFresh())
                .field("temp_indoor_c", s.getAirTempIndoor())
                .field("temp_waste_c", s.getAirTempWaste())
                .field("filter_pct", s.getFilterWearLevelPercent())
                .field("power_w", s.getPowerConsumption())
                .field("heat_recovery_w", s.getHeatRecovering())
                .field("co2_ppm", s.getWallControllerCO2())
                .field("wall_temp_c", s.getWallControllerTemperature())
                .field("time_program", s.getTimeProgramActive())
                .end();
    }
}
