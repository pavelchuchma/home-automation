package app;

import controller.actor.AbstractActor;
import node.MessageType;
import node.Node;
import nodeImpl.Node03Listener;
import nodeImpl.Node11Listener;
import org.apache.log4j.Logger;
import packet.Packet;
import packet.PacketUartIO;
import packet.ReceivedPacketHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class NodeInfoCollector {
    static Logger log = Logger.getLogger(NodeInfoCollector.class.getName());
    static NodeInfoCollector instance;

    PacketUartIO packetUartIO;
    NodeInfo[] nodeInfoArray = new NodeInfo[50];

    SwitchListener switchListener = new SwitchListener();

    public SwitchListener getSwitchListener() {
        return switchListener;
    }

    public static NodeInfoCollector getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Not created yet!");
        }
        return instance;
    }

    public void start() {
        Node node03 = new Node(3, packetUartIO);
        node03.addListener(new Node03Listener());
        addNode(node03);

        Node node11 = new Node(11, packetUartIO);
        node11.addListener(new Node11Listener());
        addNode(node11);

        packetUartIO.addReceivedPacketListener(new ReceivedPacketHandler() {
            @Override
            public void packetReceived(Packet packet) {
                NodeInfo nodeInfo = getOrCreateNodeInfo(packet);
                // set boot time in case of reboot message
                if (packet.messageType == MessageType.MSG_OnReboot) {
                    nodeInfo.setBootTime(new Date());
                }

                // Check if build time is set.
                // But do it in double-checked section to prevent parallel getBuildTime calls to one node.
                if (nodeInfo.buildTime == null && packet.messageType == MessageType.MSG_OnHeartBeat) {
                    synchronized (nodeInfo) {
                        if (nodeInfo.buildTime == null) {
                            try {
                                log.info(String.format("Getting buidtime of node: %d nodeInfo: %s", packet.nodeId, nodeInfo));
                                nodeInfo.buildTime = nodeInfo.node.getBuildTime();
                            } catch (IOException e) {
                                log.error("Cannot get build time of node #" + packet.nodeId, e);
                            }
                        }
                    }
                }
                nodeInfo.setLastPingTime(new Date());
                nodeInfo.addReceivedSentMessage(packet);
            }
        });

        packetUartIO.addSentPacketListener(new PacketUartIO.PacketSentListener() {
            @Override
            public void packetSent(Packet packet) {
                getOrCreateNodeInfo(packet).addSentLogMessage(packet);
            }
        });
    }

    public NodeInfoCollector(final PacketUartIO packetUartIO) {
        if (instance != null) {
            throw new IllegalStateException("Already created!!!");
        }
        instance = this;

        this.packetUartIO = packetUartIO;
    }

    public void addNode(Node node) {
        log.debug(String.format("Node #%d added", node.getNodeId()));

        if (nodeInfoArray[node.getNodeId()] != null) {
            nodeInfoArray[node.getNodeId()].node = node;
        } else {
            nodeInfoArray[node.getNodeId()] = new NodeInfo(node);
        }
        node.addListener(switchListener);
    }

    private NodeInfo getOrCreateNodeInfo(Packet packet) {
        if (nodeInfoArray[packet.nodeId] == null) {
            log.debug("Registering node #" + packet.nodeId);
            Node node = new Node(packet.nodeId, packetUartIO);
            addNode(node);
        }
        return nodeInfoArray[packet.nodeId];
    }

    public String getReport() {
        StringBuilder builder = new StringBuilder();

        builder.append("<html>" +
                "<meta http-equiv='refresh' content='1'/>" +
                "<head>" +
                "<link href='../report.css' rel='stylesheet' type='text/css'/>\n" +
                "</head>" +
                "<body>" +
                "<table class='nodeTable'>\n" +
                "<tr><th class=''>Node #<th class=''>Last Ping Time<th class=''>Boot Time<th class=''>Build Time<th class=''>MessageLog");

        for (NodeInfo info : nodeInfoArray) {
            if (info != null) {
                String lastPingClass = "errorValue";
                String lastPingString = "-";
                long lastPing;
                if (info.lastPingTime != null) {
                    lastPing = (new Date().getTime() - info.lastPingTime.getTime()) / 1000;
                    if (lastPing <= 10) lastPingClass = "fineValue";
                    lastPingString = lastPing + " s";
                }
                builder.append(String.format("<tr><td>%d<td class='%s'>%s<td>%s<td>%s<td class='messageLog'>", info.node.getNodeId(), lastPingClass, lastPingString, info.bootTime, info.buildTime));

                for (LogMessage m : info.getMessageLog()) {
                    builder.append(String.format("<div class='%s'>%s%s</div>",
                            (m.received) ? "receivedMessage" : "sentMessage",
                            MessageType.toString(m.packet.messageType),
                            (m.packet.data != null) ? Arrays.toString(m.packet.data) : ""));
                }
                builder.append("\n");
            }
        }
        builder.append("</table></body></html>");
        return builder.toString();
    }

    public Node getNode(int i) {
        return (nodeInfoArray[i] != null) ? nodeInfoArray[i].node : null;
    }
}