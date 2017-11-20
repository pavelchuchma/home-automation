package serial.poc;

import java.io.IOException;

public interface IPacketSource {
    PacketData getPacket() throws InterruptedException, IOException;
}
