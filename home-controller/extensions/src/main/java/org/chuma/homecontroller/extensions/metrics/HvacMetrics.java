package org.chuma.homecontroller.extensions.metrics;

import org.chuma.homecontroller.extensions.actor.HvacActor;
import org.chuma.hvaccontroller.device.HvacDevice;

/**
 * {@code hvac}: the air-conditioning unit, skipped when the bus has been silent for longer than
 * {@link #MAX_PACKET_AGE_MS} so a dead link is a gap and not the last values repeated forever.
 */
public class HvacMetrics implements MetricSource {
    static final long MAX_PACKET_AGE_MS = 5 * 60_000;

    private final HvacActor actor;
    private final HvacPacketClock clock;

    public HvacMetrics(HvacActor actor, HvacPacketClock clock) {
        this.actor = actor;
        this.clock = clock;
    }

    @Override
    public void sample(LineProtocolWriter writer, long nowMs) {
        HvacDevice d = actor.getHvacDevice();
        if (d == null || clock.getLastPacketMs() == 0 || nowMs - clock.getLastPacketMs() > MAX_PACKET_AGE_MS) {
            return;
        }
        writer.line("hvac", nowMs)
                .field("on", d.isRunning())
                .field("mode", d.getCurrentMode())
                .field("target_mode", d.getTargetMode())
                .field("fan", d.getFanSpeed())
                .field("auto", d.isAutoMode())
                .field("quiet", d.isQuiteMode())
                .field("sleep", d.isSleepMode())
                .field("defrost", d.isDefrost())
                .field("target_c", d.getTargetTemperature())
                .field("air_c", d.getAirTemperature())
                .field("air2_c", d.getAir2Temperature())
                .field("room_c", d.getRoomTemperature())
                .field("unit_c", d.getUnitTemperature())
                .end();
    }
}
