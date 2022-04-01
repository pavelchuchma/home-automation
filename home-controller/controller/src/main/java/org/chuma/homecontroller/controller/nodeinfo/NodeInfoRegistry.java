package org.chuma.homecontroller.controller.nodeinfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.node.MessageType;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.packet.IPacketUartIO;
import org.chuma.homecontroller.base.packet.Packet;

/**
 * Registry of all nodes in system. Supports autodiscovery of nodes by automatically registering
 * nodes referenced in any sent or received packet.
 */
public class NodeInfoRegistry {
    static Logger log = LoggerFactory.getLogger(NodeInfoRegistry.class.getName());

    final IPacketUartIO packetUartIO;
    final Map<Integer, NodeInfo> nodeInfoMap = new HashMap<>();
    final SwitchListener switchListener = new SwitchListener();

    public NodeInfoRegistry(final IPacketUartIO packetUartIO) {
        this.packetUartIO = packetUartIO;
    }

    /**
     * Get switch listener registered to each added node. This switch listener
     * will receive all events from the nodes.
     */
    public SwitchListener getSwitchListener() {
        return switchListener;
    }

    /**
     * Get {@link NodeInfo} for all registered nodes.
     */
    public synchronized Iterable<NodeInfo> getNodeInfos() {
        return new ArrayList<>(nodeInfoMap.values());
    }

    public void start() {
        packetUartIO.addReceivedPacketListener(new IPacketUartIO.PacketReceivedListener() {
            @Override
            public void packetReceived(Packet packet) {
                // Find or register node
                NodeInfo nodeInfo = getOrCreateNodeInfo(packet);
                // Set boot time in case of reboot message
                if (packet.messageType == MessageType.MSG_OnReboot) {
                    nodeInfo.setBootTime(new Date());
                    // invalidate build time
                    nodeInfo.setBuildTime(null);
                }

                // Check if build time is set.
                // But do it in double-checked section to prevent parallel getBuildTime calls to one node.
                if (nodeInfo.getBuildTime() == null && packet.messageType == MessageType.MSG_OnHeartBeat) {
                    //noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (nodeInfo) {
                        if (nodeInfo.getBuildTime() == null) {
                            try {
                                log.info("Getting build time of node: {} nodeInfo: {}", packet.nodeId, nodeInfo);
                                nodeInfo.setBuildTime(nodeInfo.getNode().getBuildTime());
                            } catch (IOException e) {
                                log.error("Cannot get build time of node #" + packet.nodeId, e);
                            }
                        }
                    }
                }
                // Last ping is last time something was received from the node
                nodeInfo.setLastPingTime(new Date());
                // Log received message
                nodeInfo.addReceivedSentMessage(packet);
            }

            @Override
            public void notifyRegistered(IPacketUartIO packetUartIO) {
            }
        });

        // Register sent packet listener to automatically register unknown nodes and log messages
        packetUartIO.addSentPacketListener(packet -> getOrCreateNodeInfo(packet).addSentLogMessage(packet));
    }

    /**
     * Add node to system.
     */
    public synchronized NodeInfo addNode(Node node) {
        final Integer nodeId = node.getNodeId();

        NodeInfo nodeInfo = nodeInfoMap.get(nodeId);
        if (nodeInfo != null) {
            nodeInfo.setNode(node);
        } else {
            nodeInfo = new NodeInfo(node);
            nodeInfoMap.put(nodeId, nodeInfo);
        }
        node.addListener(switchListener);
        log.debug("Node #{} added", nodeId);
        return nodeInfo;
    }

    /**
     * Create new node and add it to the system.
     */
    public Node createNode(int nodeId, String name) {
        Node node = new Node(nodeId, name, packetUartIO);
        addNode(node);
        return node;
    }

    /**
     * Get {@link NodeInfo} for node specified in packet. The node gets automatically registered if not present.
     */
    private synchronized NodeInfo getOrCreateNodeInfo(Packet packet) {
        NodeInfo nodeInfo = nodeInfoMap.get(packet.nodeId);
        if (nodeInfo != null) {
            return nodeInfo;
        }

        log.debug("Registering node #{}", packet.nodeId);
        Node node = new Node(packet.nodeId, packetUartIO);
        return addNode(node);
    }

    /**
     * Get {@link Node} instance for given node ID.
     *
     * @return node or null if not registered
     */
    public synchronized Node getNode(int nodeId) {
        NodeInfo nodeInfo = nodeInfoMap.get(nodeId);
        return (nodeInfo != null) ? nodeInfo.getNode() : null;
    }

    /**
     * Get {@link NodeInfo} for given node ID.
     *
     * @return node info or null if not registered
     */
    public synchronized NodeInfo getNodeInfo(int nodeId) {
        return nodeInfoMap.get(nodeId);
    }
}