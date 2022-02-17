package org.chuma.homecontroller.controller.nodeinfo;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.node.MessageType;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.packet.IPacketUartIO;
import org.chuma.homecontroller.base.packet.Packet;
import org.chuma.homecontroller.base.packet.PacketUartIO;

public class NodeInfoRegistry {
    static Logger log = LoggerFactory.getLogger(NodeInfoRegistry.class.getName());

    final IPacketUartIO packetUartIO;
    final Map<Integer, NodeInfo> nodeInfoMap = new HashMap<>();
    final SwitchListener switchListener = new SwitchListener();

    public NodeInfoRegistry(final IPacketUartIO packetUartIO) {
        this.packetUartIO = packetUartIO;
    }

    public SwitchListener getSwitchListener() {
        return switchListener;
    }

    public Iterable<NodeInfo> getNodeInfos() {
        return nodeInfoMap.values();
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
                    //noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (nodeInfo) {
                        if (nodeInfo.buildTime == null) {
                            try {
                                log.info("Getting build time of node: {} nodeInfo: {}", packet.nodeId, nodeInfo);
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

        packetUartIO.addSentPacketListener(packet -> getOrCreateNodeInfo(packet).addSentLogMessage(packet));
    }

    public synchronized NodeInfo addNode(Node node) {
        final Integer nodeId = node.getNodeId();

        NodeInfo nodeInfo = nodeInfoMap.get(nodeId);
        if (nodeInfo != null) {
            nodeInfo.node = node;
        } else {
            nodeInfo = new NodeInfo(node);
            nodeInfoMap.put(nodeId, nodeInfo);
        }
        node.addListener(switchListener);
        log.debug("Node #{} added", nodeId);
        return nodeInfo;
    }

    private synchronized NodeInfo getOrCreateNodeInfo(Packet packet) {
        NodeInfo nodeInfo = nodeInfoMap.get(packet.nodeId);
        if (nodeInfo != null) {
            return nodeInfo;
        }

        log.debug("Registering node #" + packet.nodeId);
        Node node = new Node(packet.nodeId, packetUartIO);
        return addNode(node);
    }

    public synchronized Node getNode(int nodeId) {
        NodeInfo nodeInfo = nodeInfoMap.get(nodeId);
        return (nodeInfo != null) ? nodeInfo.node : null;
    }

    public synchronized NodeInfo getNodeInfo(int nodeId) {
        return nodeInfoMap.get(nodeId);
    }

    public Node createNode(int i, String name) {
        Node node = new Node(i, name, packetUartIO);
        addNode(node);
        return node;
    }
}