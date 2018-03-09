package serial.poc;

import java.io.IOException;

import serial.poc.Packet.Packet;

public interface IPacketProcessor {
    void start() throws IOException;

    void stop() throws IOException;

    void process(Packet packetData) throws IOException;
}
