package serial.poc.Packet;

import java.io.IOException;
import java.util.Collection;

import serial.poc.IPacketProcessor;
import serial.poc.IPacketSource;
import serial.poc.PacketData;

public class PacketConsumer {
    Collection<IPacketProcessor> processors;

    public PacketConsumer(Collection<IPacketProcessor> processors) {
        this.processors = processors;
    }

    public void consume(IPacketSource source) throws IOException, InterruptedException {
        for (IPacketProcessor proc : processors) {
            proc.start();
        }
        while (true) {
            PacketData packetData = source.getPacket();
            if (packetData == null) {
                break;
            }
            Packet packet = PacketFactory.Deserialize(packetData);
            for (IPacketProcessor proc : processors) {
                proc.process(packet);
            }
        }
        for (IPacketProcessor proc : processors) {
            proc.stop();
        }
    }
}
