/*
 * Copyright 2002-2022 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * https://www.ataccama.com
 */

package org.chuma.homecontroller.base.packet.simulation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.Validate;
import org.slf4j.event.Level;

import org.chuma.homecontroller.base.node.MessageType;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.Pic;
import org.chuma.homecontroller.base.node.Pin;
import org.chuma.homecontroller.base.packet.AbstractPacketUartIO;
import org.chuma.homecontroller.base.packet.Packet;
import org.chuma.homecontroller.base.packet.PacketUartIO;

/**
 * Implementation of {@link PacketUartIO} which simulates the actual system.
 */
public class SimulatedPacketUartIO extends AbstractPacketUartIO {
    public static final int[] TRIS_ADDRESS = new int[] { Pic.TRISA, Pic.TRISB, Pic.TRISC };
    public static final int[] PORT_ADDRESS = new int[] { Pic.PORTA, Pic.PORTB, Pic.PORTC };

    private ConcurrentMap<Integer, SimulatedNode> nodes = new ConcurrentHashMap<>();
    private AggregateListener listener = new AggregateListener();
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    private boolean running;

    @Override
    public void start() throws IOException {
        Validate.isTrue(!running, "Already running");
        running = true;
        for (SimulatedNode n : nodes.values()) {
            sendReboot(n);
        }
        // Heartbeat thread
        new Thread(() -> {
            while (running) {
                for (SimulatedNode n : nodes.values()) {
                    if (n.shouldSendHeartBeat()) {
                        sendHeartBeat(n);
                    }
                }
                try {
                Thread.sleep(500);
                } catch (InterruptedException e) {
                }
            }
        }).start();
    }

    @Override
    public void close() {
        Validate.isTrue(running, "Not running");
        running = false;
    }

    @Override
    protected void sendImpl(Packet packet) throws IOException {
        int nodeId = packet.nodeId;
        int[] data = packet.data;
        // This method is synchronized externally - we can do unsynchronized modifications to "nodes" freely
        SimulatedNode node = nodes.get(nodeId);
        if (node == null) {
            // Unknown node - register it and generate reboot message if running
            node = newSimulatedNode(nodeId, null, listener);
            nodes.put(nodeId, node);
            if (running) {
                sendReboot(node);
            }
        }
        switch (packet.messageType) {
            case MessageType.MSG_ReadRamRequest: {
                assertLength(packet, 4);
                int address = data[0] | (data[1] << 8);
                int v = node.readRam(address);
                listener.logMessage(node, Level.INFO, "read RAM at %s -> 0x%02x (%s, %d)", Pic.toString(address), v, Node.asBinary(v), v);
                sendSimulatedPacket(new Packet(nodeId, MessageType.MSG_ReadRamResponse, new int[] { v }), 50);
                break;
            }
            case MessageType.MSG_SetPortD:
                // TODO: No address constants for port D - need to add to TRIS_ADDRESS and PORT_ADDRESS and initialize in SimulatedNode constructor
                throw new IllegalArgumentException("Port D is NOT supported");
            case MessageType.MSG_SetPortA:
            case MessageType.MSG_SetPortB:
            case MessageType.MSG_SetPortC: {
                int port = packet.messageType - MessageType.MSG_SetPortA;
                // There are three variants of this message
                if (packet.length < 4) {
                    // Too short
                    assertLength(packet, 4);
                }
                // Prepare value to set - read original value and apply new value with mask
                // But do not set right now - only after TRIS/event write
                int v = (node.readRam(PORT_ADDRESS[port]) & ~data[0]) | (data[1] & data[0]);
                if (packet.length == 6) {
                    // Set TRIS
                    listener.logMessage(node, Level.INFO, "Set port %c - value %s (mask %s), event mask %s, TRIS %s",
                            (char)(port + 'A'), Node.asBinary(data[1]), Node.asBinary(data[0]), Node.asBinary(data[2]), Node.asBinary(data[3]));
                    int t = node.writeRam(TRIS_ADDRESS[port], data[3]);
                    node.setEventMask(port, data[2]);
                    v = node.writeRam(PORT_ADDRESS[port], v);
                    sendSimulatedPacket(new Packet(nodeId, MessageType.MSG_SetPortResponse, new int[] { packet.messageType, v, t }), 50);
                } else if (packet.length == 5) {
                    // Set event mask
                    listener.logMessage(node, Level.INFO, "Set port %c - value %s (mask %s), event mask %s",
                            (char)(port + 'A'), Node.asBinary(data[1]), Node.asBinary(data[0]), Node.asBinary(data[2]));
                    node.setEventMask(port, data[2]);
                    node.writeRam(PORT_ADDRESS[port], v);
                    sendSimulatedPacket(new Packet(nodeId, MessageType.MSG_SetPortResponse, new int[] { packet.messageType, v }), 50);
                } else {
                    // Set value
                    listener.logMessage(node, Level.INFO, "Set port %c - value %s (mask %s)",
                            (char)(port + 'A'), Node.asBinary(data[1]), Node.asBinary(data[0]));
                    node.writeRam(PORT_ADDRESS[port], v);
                    sendSimulatedPacket(new Packet(nodeId, MessageType.MSG_SetPortResponse, new int[] { packet.messageType, v }), 50);
                }
                break;
            }
            case MessageType.MSG_SetManualPwmValueRequest: {
                assertLength(packet, 4);
                int port = data[0] & 0xf;
                int pin = (data[0] >> 4) & 0xf;
                int v = data[1];
                listener.logMessage(node, Level.INFO, "Set manual PWM - port %c, pin %d - %d (%d%%)", (char)(port + 'A'), pin, v, v * 100 / 48);
                int res = node.setManualPwm(port, pin, v);
                sendSimulatedPacket(new Packet(nodeId, MessageType.MSG_SetManualPwmValueResponse, new int[] { res }), 50);
                break;
            }
            case MessageType.MSG_SetHeartBeatPeriod: {
                assertLength(packet, 3);
                listener.logMessage(node, Level.INFO, "Set heartbeat period to %ds", data[0]);
                node.setHeartBeatPeriod(data[0]);
                break;
            }
            case MessageType.MSG_SetFrequencyRequest: {
                assertLength(packet, 4);
                listener.logMessage(node, Level.INFO, "Set CPU frequency to %dMHz, baud rate prescaller %d", data[0], data[1]);
                sendSimulatedPacket(new Packet(nodeId, MessageType.MSG_SetFrequencyResponse, new int[0]), 50);
                break;
            }
            case MessageType.MSG_InitializationFinished: {
                assertLength(packet, 2);
                listener.logMessage(node, Level.INFO, "Initialization finished");
                sendHeartBeat(node);
                break;
            }
            case MessageType.MSG_GetBuildTimeRequest: {
                assertLength(packet, 2);
                listener.logMessage(node, Level.INFO, "Get build time");
                sendSimulatedPacket(new Packet(nodeId, MessageType.MSG_GetBuildTimeResponse, new int[] { 15, 2, 22, 10, 20 }), 50);
                break;
            }
            default:
                listener.logMessage(node, Level.WARN, "Unsupported message: %s", packet);
                break;
        }
    }

    /**
     * Register simulated node change listener.
     */
    public void addListener(SimulatedNodeListener listener) {
        this.listener.addListener(listener);
    }

    /**
     * Get simulated node with given ID or null if not registered.
     */
    public SimulatedNode getSimulatedNode(int id) {
        return nodes.get(id);
    }

    /**
     * Get all registered simulated nodes.
     */
    public List<SimulatedNode> getSimulatedNodes() {
        return new ArrayList<>(nodes.values());
    }

    /**
     * Register node to handle. This call is required before {@link #start()} since otherwise
     * the node won't send OnReboot message and won't be initialized.
     */
    public void registerNode(int id) {
        nodes.put(id, newSimulatedNode(id, null, listener));
    }

    /**
     * Register node to handle. This call is required before {@link #start()} since otherwise
     * the node won't send OnReboot message and won't be initialized.
     */
    public void registerNode(Node node) {
        nodes.put(node.getNodeId(), newSimulatedNode(node.getNodeId(), node, listener));
    }

    /**
     * Create new simulated node instance. {@link Node} instance is optional.
     */
    protected SimulatedNode newSimulatedNode(int id, Node node, SimulatedNodeListener listener) {
        return new SimulatedNode(this, id, node, listener);
    }

    private void assertLength(Packet packet, int expLen) {
        Validate.isTrue(packet.length == expLen, "%s request length wrong: %d (expected %d)", MessageType.toString(packet.messageType), packet.length, expLen);
    }

    private synchronized void sendSimulatedPacket(Packet packet, int timeoutMs) {
        long when = System.currentTimeMillis() + timeoutMs;
        // We send it in different thread for two reasons:
        // - not to block calling thread when there is timeout set
        // - not to send in the same thread as the thread handling send() in case of response
        // TODO: Better to use some scheduler and less threads...
        threadPool.execute(() -> {
            long wait = when - System.currentTimeMillis();
            if (wait >= 0) {
                try {
                    Thread.sleep(wait);
                } catch(InterruptedException e) {
                }
            }
            processPacket(packet);
        });
    }

    private void sendReboot(SimulatedNode node) {
        // TODO: Data unused in application - just send something
        listener.logMessage(node, Level.INFO, "Sending reboot signal");
        sendSimulatedPacket(new Packet(node.getId(), MessageType.MSG_OnReboot, new int[] { 0, 0 }), 10);
    }

    private void sendHeartBeat(SimulatedNode node) {
        node.resetHeartBeat();
        // TODO: Data unused in application - just send 1
        listener.logMessage(node, Level.INFO, "Sending heartbeat");
        sendSimulatedPacket(new Packet(node.getId(), MessageType.MSG_OnHeartBeat, new int[] { 1 }), 0);
    }

    /**
     * Send port pin change event to application.
     */
    public void sendPortPinChange(SimulatedNode node, Pin pin, int eventMask, int value) {
        listener.logMessage(node, Level.INFO, "Sending pin %s change event, event mask %s, value %s", pin, Node.asBinary(eventMask), Node.asBinary(value));
        sendSimulatedPacket(new Packet(node.getId(), MessageType.MSG_OnPortAPinChange + pin.getPortIndex(), new int[] { eventMask, value }), 0);
    }

    private static class AggregateListener implements SimulatedNodeListener {
        private final List<SimulatedNodeListener> listeners = new ArrayList<>();

        public void addListener(SimulatedNodeListener listener) {
            listeners.add(listener);
        }

        @Override
        public void logMessage(SimulatedNode node, Level level, String messageFormat, Object... args) {
            for (SimulatedNodeListener l : listeners) {
                l.logMessage(node, level, messageFormat, args);
            }
            Object[] nargs = new Object[args.length + 2];
            nargs[0] = level;
            nargs[1] = node.getId();
            System.arraycopy(args, 0, nargs, 2, args.length);
            System.out.printf("%5s: %d: " + messageFormat + "\n", nargs); // TODO
        }

        @Override
        public void onSetPort(SimulatedNode node, int port, int value) {
            for (SimulatedNodeListener l : listeners) {
                l.onSetPort(node, port, value);
            }
        }

        @Override
        public void onSetTris(SimulatedNode node, int port, int value) {
            for (SimulatedNodeListener l : listeners) {
                l.onSetTris(node, port, value);
            }
        }

        @Override
        public void onSetEventMask(SimulatedNode node, int port, int mask) {
            for (SimulatedNodeListener l : listeners) {
                l.onSetEventMask(node, port, mask);
            }
        }

        @Override
        public void onSetManualPwm(SimulatedNode node, int port, int pin, int value) {
            for (SimulatedNodeListener l : listeners) {
                l.onSetManualPwm(node, port, pin, value);
            }
        }
    }
}
