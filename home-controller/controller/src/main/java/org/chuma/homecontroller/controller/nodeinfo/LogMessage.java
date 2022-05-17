package org.chuma.homecontroller.controller.nodeinfo;

import org.chuma.homecontroller.base.packet.Packet;

/**
 * Sent or received message kept in {@link NodeInfo} log.
 */
public class LogMessage {
    public final boolean received;
    public final Packet packet;
    public final long timestamp;

    public LogMessage(Packet packet, boolean received) {
        this.received = received;
        this.packet = packet;
        timestamp = System.currentTimeMillis();
    }
}