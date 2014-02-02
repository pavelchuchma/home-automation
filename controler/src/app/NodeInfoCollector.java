package app;

import controller.actor.AbstractActor;
import controller.Switch;
import node.MessageType;
import node.Node;
import node.Pin;
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


    PacketUartIO packetUartIO;
    NodeInfo[] nodeInfoArray = new NodeInfo[50];

    ConcurrentHashMap<String, Switch> switchMap = new ConcurrentHashMap<String, Switch>();
    ConcurrentHashMap<String, AbstractActor> actorMap = new ConcurrentHashMap<String, AbstractActor>();
    Node.Listener genericNodeListener = createGenericNodeListener();

    void addSwitch(Switch sw) {
        switchMap.put(createNodePinKey(sw.getNodeId(), sw.getPin()), sw);
    }

    public NodeInfoCollector(final PacketUartIO packetUartIO) {
        this.packetUartIO = packetUartIO;


        Node node03 = new Node(3, packetUartIO);
        node03.addListener(new Node03Listener(this));
        addNode(node03);


        Node node11 = new Node(11, packetUartIO);
        node11.addListener(new Node11Listener(this));
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
                                log.info("Getting buidTime node: " + packet.nodeId + " nodeInfo: " + nodeInfo);
                                nodeInfo.buildTime = nodeInfo.node.getBuildTime();
                                //nodeInfo.node.echo((char) (nodeInfo.node.getNodeId()), counter++);
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

    public void addNode(Node node) {
        log.debug("Node #" + node.getNodeId() + " added");
        node.addListener(genericNodeListener);

        if (nodeInfoArray[node.getNodeId()] != null) {
            nodeInfoArray[node.getNodeId()].node = node;
        } else {
            nodeInfoArray[node.getNodeId()] = new NodeInfo(node);
        }
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

    public static String createNodePinKey(int nodeId, Pin pin) {
        return String.format("%d:%s", nodeId, pin);
    }

    private Node.Listener createGenericNodeListener() {
        return new Node.Listener() {
            @Override
            public void onButtonDown(Node node, Pin pin) {
                String swKey = createNodePinKey(node.getNodeId(), pin);
                Switch sw = switchMap.get(swKey);
                if (sw != null) {

                }
            }

            @Override
            public void onButtonUp(Node node, Pin pin, int downTime) {
            }

            @Override
            public void onReboot(Node node, int pingCounter, int rconValue) throws IOException, IllegalArgumentException {

                int inputMasks = 0x00000000;
                int outputMasks = 0x00000000;
                // go through all switches to get initial settings
                for (Switch sw : switchMap.values()) {
                    if (node.getNodeId() == sw.getNodeId()) {
                        // todo: presunut rotaci jako actor
                        inputMasks |= 2 << sw.getPin().ordinal();
                    }
                }
                for (AbstractActor act : actorMap.values()) {
                    if (node.getNodeId() == act.getNodeId()) {
                        inputMasks |= act.getPinOutputMask();
                    }
                }

                for (int i = 0; i < 4; i++) {
                    int eventMask = (inputMasks >> i * 8) & 0xFF;
                    if (eventMask != 0) {
                        // todo: check TRIS to don't break CAN/UART settings
                        node.setPortValue((char) ('A' + i), 0x00, 0x00, eventMask, 0xFF);
                    }
                }
            }
        };
    }
}