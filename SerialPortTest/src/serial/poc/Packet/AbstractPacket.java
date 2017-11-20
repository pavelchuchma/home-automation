package serial.poc.Packet;

import serial.poc.PacketData;

public abstract class AbstractPacket implements Packet {
    PacketData packetData;

    public AbstractPacket(PacketData packetData) {
        this.packetData = packetData;
    }


    @Override
    public PacketData getData() {
        return packetData;
    }

    @Override
    public boolean isRequest() {
        return packetData.isRequest();
    }

    @Override
    public int getFrom() {
        return packetData.from;
    }

    @Override
    public int getTo() {
        return packetData.to;
    }

    @Override
    public int getCommand() {
        return packetData.command;
    }

    protected int boolAsInt(boolean b) {
        return (b) ? 1 : 0;
    }

    @Override
    public String toString() {
        return "";
    }
}
