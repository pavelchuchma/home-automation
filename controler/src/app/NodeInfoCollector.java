package app;

import node.MessageType;
import node.Node;
import org.apache.log4j.Logger;
import packet.Packet;
import packet.PacketUartIO;
import packet.ReceivedPacketHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NodeInfoCollector {
    static Logger log = Logger.getLogger(NodeInfoCollector.class.getName());
    PacketUartIO packetUartIO;
    final Map<Integer, NodeInfo> nodes = new ConcurrentHashMap<Integer, NodeInfo>();

    public NodeInfoCollector(final PacketUartIO packetUartIO) {
        this.packetUartIO = packetUartIO;

        packetUartIO.addReceivedPacketListener(new ReceivedPacketHandler() {
            @Override
            public void packetReceived(Packet packet) {
                if (packet.messageType == MessageType.MSG_OnReboot || packet.messageType == MessageType.MSG_OnHeartBeat) {
                    NodeInfo nodeInfo = nodes.get(packet.nodeId);
                    if (nodeInfo == null || packet.messageType == MessageType.MSG_OnReboot) {
                        // on reboot, or no such node present
                        nodeInfo = registerNewNode(packet, packetUartIO);
                    }
                    // check build time is set
                    if (nodeInfo.buildTime == null) {
                        try {
                            nodeInfo.buildTime = nodeInfo.node.getBuildTime();
                        } catch (IOException e) {
                            log.error("Cannot get build time of node #" + packet.nodeId, e);
                        }
                    }
                    nodeInfo.setLastPingTime(new Date());
                }
            }
        });
    }

    private NodeInfo registerNewNode(Packet packet, PacketUartIO packetUartIO) {
        nodes.remove(packet.nodeId);

        log.debug("Registering node #" + packet.nodeId);
        Node node = new Node(packet.nodeId, packetUartIO);
        Date buildTime = null;
        NodeInfo nodeInfo = new NodeInfo(node, buildTime);
        if (packet.messageType == MessageType.MSG_OnReboot) {
            nodeInfo.setBootTime(new Date());
        }
        nodes.put(packet.nodeId, nodeInfo);
        return nodeInfo;
    }

    public String getReport() {
        StringBuilder builder = new StringBuilder();

        builder.append("<html>" +
                "<meta http-equiv='refresh' content='1' >" +
                "<body>" +
                "<table class=''>\n" +
                "<tr><td class=''>Node #<td class=''>Last Ping Time<td class=''>Boot Time<td class=''>Build Time");


        Set<Integer> idSet = nodes.keySet();
        Integer[] idArray = idSet.toArray(new Integer[idSet.size()]);
        Arrays.sort(idArray);
        for (Integer i : idArray) {
            NodeInfo info = nodes.get(i);
            if (info != null) {
                builder.append(String.format("<tr><td>%d<td>%s s<td>%s<td>%s\n", info.node.getNodeId(), (info.lastPingTime != null) ? (new Date().getTime() - info.lastPingTime.getTime()) / 1000 : '-', info.bootTime, info.buildTime));
            }
        }
        builder.append("</table></body></html>");
        return builder.toString();
    }
}