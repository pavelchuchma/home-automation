package org.chuma.homecontroller.extensions.metrics;

import org.chuma.hvaccontroller.IPacketProcessor;
import org.chuma.hvaccontroller.packet.Packet;

/**
 * Remembers when the HVAC bus last delivered a packet. {@code HvacDevice} keeps returning its last values
 * after the bus goes silent; this is what tells them apart from live ones.
 */
public class HvacPacketClock implements IPacketProcessor {
    private volatile long lastPacketMs;

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void process(Packet packet) {
        lastPacketMs = System.currentTimeMillis();
    }

    /**
     * @return time of the last packet in ms since epoch, 0 before the first one
     */
    public long getLastPacketMs() {
        return lastPacketMs;
    }
}
