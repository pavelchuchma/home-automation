package org.chuma.homecontroller.controller.nodeinfo;

import org.chuma.homecontroller.nodes.packet.Packet;

import java.util.Date;

public class LogMessage {
    public final boolean received;
    public final Packet packet;
    public final long receivedDate;

    public LogMessage(Packet packet, boolean received) {
        this.received = received;
        this.packet = packet;
        receivedDate = new Date().getTime();
    }
}