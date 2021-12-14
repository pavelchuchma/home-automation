package app;

import controller.action.Action;
import node.MessageType;
import node.Node;
import org.apache.log4j.Logger;
import packet.IPacketUartIO;
import packet.Packet;
import packet.PacketUartIO;
import servlet.Servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

public class NodeInfoCollector implements Iterable<NodeInfo> {
    public static final int MAX_HEART_BEAT_PERIOD = 10;
    static Logger log = Logger.getLogger(NodeInfoCollector.class.getName());
    static NodeInfoCollector instance;

    IPacketUartIO packetUartIO;
    NodeInfo[] nodeInfoArray = new NodeInfo[70];

    SwitchListener switchListener = new SwitchListener();

    public NodeInfoCollector(final IPacketUartIO packetUartIO) {
        if (instance != null) {
            throw new IllegalStateException("Already created!!!");
        }
        instance = this;

        this.packetUartIO = packetUartIO;
    }

    public static NodeInfoCollector getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Not created yet!");
        }
        return instance;
    }

    public SwitchListener getSwitchListener() {
        return switchListener;
    }

    public void start() {
        packetUartIO.addReceivedPacketListener(new PacketUartIO.PacketReceivedListener() {
            @Override
            public void packetReceived(Packet packet) {
                NodeInfo nodeInfo = getOrCreateNodeInfo(packet);
                // set boot time in case of reboot message
                if (packet.messageType == MessageType.MSG_OnReboot) {
                    nodeInfo.setBootTime(new Date());
                    // invalidate build time
                    nodeInfo.buildTime = null;
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

            @Override
            public void notifyRegistered(PacketUartIO packetUartIO) {
            }
        });

        packetUartIO.addSentPacketListener(new PacketUartIO.PacketSentListener() {
            @Override
            public void packetSent(Packet packet) {
                getOrCreateNodeInfo(packet).addSentLogMessage(packet);
            }
        });
    }

    public synchronized void addNode(Node node) {
        log.debug(String.format("Node #%d added", node.getNodeId()));

        if (nodeInfoArray[node.getNodeId()] != null) {
            nodeInfoArray[node.getNodeId()].node = node;
        } else {
            nodeInfoArray[node.getNodeId()] = new NodeInfo(node);
        }
        node.addListener(switchListener);
    }

    private synchronized NodeInfo getOrCreateNodeInfo(Packet packet) {
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
                "<meta http-equiv='refresh' content='1;url=/'/>" +
                "<head>" +
                "<link href='report.css' rel='stylesheet' type='text/css'/>\n" +
                "</head>" +
                "<body>");

        String[] actionNames = new String[]{"Bzucak", "Garaz","Jidelna", "Zvuk"};
        Action[] actions = new Action[]{Servlet.action1, Servlet.action1, Servlet.action2, Servlet.action3, Servlet.action4};
        for (int i = 0; i < actionNames.length; i++) {
            if (actions[i] != null) {
                builder.append(String.format("<a href='/a%d'>%s</a>&nbsp;&nbsp;&nbsp;&nbsp;", i + 1, actionNames[i]));
            }
        }

        builder.append("<a href='" + Servlet.TARGET_LOUVERS + "'>Zaluzie...</a>&nbsp;&nbsp;&nbsp;&nbsp;");
        builder.append("<a href='" + Servlet.TARGET_LIGHTS + "'>Svetla...</a>&nbsp;&nbsp;&nbsp;&nbsp;");
        builder.append("<a href='" + Servlet.TARGET_SYSTEM + "'>System...</a>&nbsp;&nbsp;&nbsp;&nbsp;");
        builder.append("<a href='" + Servlet.TARGET_PIR_STATUS_PAGE + "'>Pir Status...</a>&nbsp;&nbsp;&nbsp;&nbsp;");
        builder.append("<a href='" + Servlet.TARGET_LIGHTS_OBYVAK + "'>Obyvak...</a>&nbsp;&nbsp;&nbsp;&nbsp;");

        builder.append("<table class='nodeTable'>\n" +
                "<tr><th class=''>Node #<th class=''>Last Ping Time<th class=''>Boot Time<th class=''>Build Time<th class=''>MessageLog");

        for (NodeInfo info : nodeInfoArray) {
            if (info != null) {
                String lastPingClass = "errorValue";
                String lastPingString = "-";
                long lastPing;
                if (info.lastPingTime != null) {
                    lastPing = (new Date().getTime() - info.lastPingTime.getTime()) / 1000;
                    if (lastPing <= Node.HEART_BEAT_PERIOD) lastPingClass = "fineValue";
                    lastPingString = lastPing + " s";
                }
                builder.append(String.format("<tr><td>%d-%s<td class='%s'>%s<td>%s<td>%s<td class='messageLog'>", info.node.getNodeId(), info.node.getName(), lastPingClass, lastPingString, info.bootTime, info.buildTime));

                for (LogMessage m : info.getMessageLog()) {
                    builder.append(String.format("<div class='%s'>%s%s</div>",
                            (m.received) ? "receivedMessage" : "sentMessage",
                            MessageType.toString(m.packet.messageType),
                            (m.packet.data != null) ? Arrays.toString(m.packet.data) : ""));
                }
                builder.append("\n");
            }
        }
        builder.append("</table>");
        builder.append("</body></html>");
        return builder.toString();
    }

    public synchronized Node getNode(int i) {
        return (nodeInfoArray[i] != null) ? nodeInfoArray[i].node : null;
    }

    public synchronized NodeInfo getNodeInfo(int i) {
        return nodeInfoArray[i];
    }

    public Node createNode(int i, String name) {
        Node node = new Node(i, name, packetUartIO);
        addNode(node);
        return node;
    }

    @Override
    public Iterator<NodeInfo> iterator() {
        return new NodeInfoIterator();  //To change body of implemented methods use File | Settings | File Templates.
    }

    class NodeInfoIterator implements Iterator<NodeInfo> {
        int position = 0;

        @Override
        public boolean hasNext() {
            for (int i = position; i < nodeInfoArray.length; i++) {
                if (nodeInfoArray[i] != null) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public NodeInfo next() {
            for (; position < nodeInfoArray.length; position++) {
                if (nodeInfoArray[position] != null) {
                    return nodeInfoArray[position++];
                }
            }
            throw new IllegalStateException("Calling next when hasNext returned false");
        }

        @Override
        public void remove() {
            throw new IllegalStateException("Remove not implemented");
        }
    }
}