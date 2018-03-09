package chuma.hvaccontroller;

import java.io.IOException;

import chuma.hvaccontroller.packet.Packet;

public interface IPacketProcessor {
    void start() throws IOException;

    void stop() throws IOException;

    void process(Packet packetData) throws IOException;
}
