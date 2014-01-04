package app;

import packet.Packet;

import java.util.Date;

public class LogMessage {
    boolean received;
    Packet packet;
    long receivedDate;

    public LogMessage(Packet packet, boolean received) {
        this.received = received;
        this.packet = packet;
        receivedDate = new Date().getTime();
    }
}