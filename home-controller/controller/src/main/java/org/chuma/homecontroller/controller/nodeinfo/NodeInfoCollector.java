package org.chuma.homecontroller.controller.nodeinfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.chuma.homecontroller.base.node.MessageType;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.packet.IPacketUartIO;
import org.chuma.homecontroller.base.packet.Packet;
import org.chuma.homecontroller.base.packet.PacketUartIO;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

public class NodeInfoCollector implements Iterable<NodeInfo> {
    static Logger log = LoggerFactory.getLogger(NodeInfoCollector.class.getName());
    static NodeInfoCollector instance;

    IPacketUartIO packetUartIO;
    //TODO: convert to int->NodeInfo map
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

    public NodeInfo[] getNodeInfoArray() {
        return nodeInfoArray;
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