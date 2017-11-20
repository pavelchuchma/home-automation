package serial.poc.Packet;

import serial.poc.PacketData;

public interface Packet {
    PacketData getData();

    boolean isRequest();

    int getFrom();

    int getTo();

    int getCommand();

    int[] getUnderstandMask();
}
