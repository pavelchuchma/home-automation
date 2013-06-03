package packet;

import node.MessageType;
import org.apache.log4j.Logger;

public class ReceivedPacketHandler implements PacketUartIO.PacketReceivedListener {
    static Logger log = Logger.getLogger(ReceivedPacketHandler.class.getName());
    PacketUartIO packetUartIO;

    @Override
    public void packetReceived(Packet packet) {
        log.debug("packetReceived: " + packet);

        if (packet.messageType == MessageType.MSG_OnPortBPinChange) {

        } else if(packet.messageType == MessageType.MSG_OnHeartBeat) {
            if (packet.nodeId == 2 && packet.data[0] == 5) {
/*
                try {
                    packetUartIO.send(packet.Packet.createMsgSetCounter(2, 100));
                } catch (IOException e) {
                    log.error("Error sending " + packet, e);
                }
*/
            }

        }
    }

    @Override
    public void notifyRegistered(PacketUartIO packetUartIO) {
        log.debug("notifyRegistered: " + packetUartIO);
        this.packetUartIO = packetUartIO;
    }
}