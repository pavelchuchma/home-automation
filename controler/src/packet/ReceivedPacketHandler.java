package packet;

import node.MessageType;
import org.apache.log4j.Logger;

public class ReceivedPacketHandler implements PacketUartIO.PacketReceivedListener {
    static Logger log = Logger.getLogger(ReceivedPacketHandler.class.getName());
    PacketUartIO packetUartIO;

    @Override
    public void packetReceived(Packet packet) {
        log.debug("packetReceived: " + packet);
    }

    @Override
    public void notifyRegistered(PacketUartIO packetUartIO) {
        log.debug("notifyRegistered: " + packetUartIO);
        this.packetUartIO = packetUartIO;
    }
}