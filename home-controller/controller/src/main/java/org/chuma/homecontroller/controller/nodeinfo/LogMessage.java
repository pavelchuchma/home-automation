package org.chuma.homecontroller.controller.nodeinfo;

import org.chuma.homecontroller.base.packet.Packet;

public class LogMessage {
    public final boolean received;
    public final Packet packet;
    public final long receivedDate;

    public LogMessage(Packet packet, boolean received) {
        this.received = received;
        this.packet = packet;
        receivedDate = System.currentTimeMillis();
    }
}