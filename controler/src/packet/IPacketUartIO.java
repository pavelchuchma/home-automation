package packet;

import java.io.IOException;

public interface IPacketUartIO {
    void addReceivedPacketListener(PacketUartIO.PacketReceivedListener listener);

    void addSpecificReceivedPacketListener(PacketUartIO.PacketReceivedListener listener, int nodeId, int messageType);

    void addSentPacketListener(PacketUartIO.PacketSentListener listener);

    void send(Packet packet) throws IOException;

    Packet send(Packet packet, int responseType, int timeout) throws IOException;

    void close();

    // Listener interface
    public interface PacketReceivedListener {
        void packetReceived(Packet packet);

        void notifyRegistered(PacketUartIO packetUartIO);
    }

    public interface PacketSentListener {
        void packetSent(Packet packet);
    }
}
