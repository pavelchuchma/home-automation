package chuma.hvaccontroller;

import java.io.IOException;

import chuma.hvaccontroller.packet.PacketData;

public interface IPacketSource {
    PacketData getPacket() throws InterruptedException, IOException;
}
