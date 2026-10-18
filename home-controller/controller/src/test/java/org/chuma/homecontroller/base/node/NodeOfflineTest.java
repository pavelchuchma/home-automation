package org.chuma.homecontroller.base.node;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.chuma.homecontroller.base.packet.Packet;
import org.chuma.homecontroller.base.packet.PacketUartIOMock;
import org.chuma.homecontroller.controller.device.AbstractConnectedDevice;

public class NodeOfflineTest {
    private static void assertNodeConfigException(ThrowingRunnable f, String expectedCode) {
        NodeConfigurationException e = assertThrows(NodeConfigurationException.class, f);
        assertEquals(expectedCode, e.getCode());
    }

    @Test
    public void addDeviceFailDeviceEventOutputClash() {
        Node a = new Node(33, "testA", new PacketUartIOMock());
        assertNodeConfigException(
                () -> a.addDevice(new TestDevice(3, 0x010203FF, 0xFF)),
                "DeviceEventOutputClash");
    }

    @Test
    public void addDeviceOk1() {
        Node a = new Node(33, "testA", new PacketUartIOMock());
        a.addDevice(new TestDevice(3, 0x0102031F, 0xE0));
    }

    @Test
    public void addDeviceFailConnectorAlreadyUsed() {
        Node a = new Node(33, "testA", new PacketUartIOMock());
        a.addDevice(new TestDevice(3, 0x00, 0xE0));
        assertNodeConfigException(
                () -> a.addDevice(new TestDevice(3, 0x00, 0xE000)),
                "ConnectorAlreadyUsed");
    }

    @Test
    public void addDeviceFailConflictingEventMask() {
        Node a = new Node(33, "testA", new PacketUartIOMock());
        a.addDevice(new TestDevice(3, 0x0102031F, 0xE0));
        assertNodeConfigException(
                () -> a.addDevice(new TestDevice(2, 0x01000000, 0xE000)),
                "ConflictingEventMask");
    }

    @Test
    public void addDeviceFailConflictingOutputMask() {
        Node a = new Node(33, "testA", new PacketUartIOMock());
        a.addDevice(new TestDevice(1, 0x0102_0000, 0xC000));
        a.addDevice(new TestDevice(3, 0x0000_001F, 0x2000));
        assertNodeConfigException(
                () -> a.addDevice(new TestDevice(2, 0xFEFD_00E0, 0x8000)),
                "ConflictingOutputMask");
    }

    @Test
    public void addDeviceFailDeviceEventMaskConflictsExistingOutputMasks() {
        Node a = new Node(33, "testA", new PacketUartIOMock());
        a.addDevice(new TestDevice(1, 0x0102_0000, 0xC000));
        a.addDevice(new TestDevice(3, 0x0000_001F, 0x2000));
        assertNodeConfigException(
                () -> a.addDevice(new TestDevice(2, 0x2000, 0x0000)),
                "DeviceEventMaskConflictsExistingOutputMasks");
    }

    @Test
    public void addDeviceFailDeviceOutputMaskConflictsExistingEventMasks() {
        Node a = new Node(33, "testA", new PacketUartIOMock());
        a.addDevice(new TestDevice(1, 0x0102_0000, 0xC000));
        a.addDevice(new TestDevice(3, 0x0000_001F, 0x2000));
        assertNodeConfigException(
                () -> a.addDevice(new TestDevice(2, 0x0000, 0x0002_0000)),
                "DeviceOutputMaskConflictsExistingEventMasks");
    }

    @Test
    public void addDeviceFailBridgeConnector3UsesReservedPins() {
        Node bridge = new Node(1, "bridge", new PacketUartIOMock());
        assertNodeConfigException(
                () -> bridge.addDevice(new TestDevice(3, connectorMask(3), 0)),
                "DeviceUsesReservedPins");
    }

    @Test
    public void addDeviceBridgeConnectors1And2Ok() {
        Node bridge = new Node(1, "bridge", new PacketUartIOMock());
        bridge.addDevice(new TestDevice(1, connectorMask(1), 0));
        bridge.addDevice(new TestDevice(2, 0, connectorMask(2)));
    }

    @Test
    public void addDeviceConnector3OkOnRegularNode() {
        Node a = new Node(33, "testA", new PacketUartIOMock());
        a.addDevice(new TestDevice(3, connectorMask(3), 0));
    }

    @Test
    public void addDeviceFailCanPins() {
        Node a = new Node(33, "testA", new PacketUartIOMock());
        assertNodeConfigException(
                () -> a.addDevice(new TestDevice(1, 0, Pin.pinB2.getBitMask() << 8)),
                "DeviceUsesReservedPins");
    }

    @Test
    public void initializeBridgeKeepsReservedTris() {
        RecordingPacketUartIO packetUartIO = new RecordingPacketUartIO();
        Node bridge = new Node(1, "bridge", packetUartIO);
        bridge.addDevice(new TestDevice(1, connectorMask(1), 0));
        bridge.addDevice(new TestDevice(2, 0, connectorMask(2)));
        bridge.initialize();

        // port B: B4, B5 inputs, B1 (LED) and B2 (CAN TX) outputs, B3 (CAN RX) input
        assertEquals(0b11111001, packetUartIO.getTris(1, 'B'));
        // port C: C0..C3 outputs, C6 (UART TX) output, C7 (UART RX) input
        assertEquals(0b10110000, packetUartIO.getTris(1, 'C'));
    }

    @Test
    public void initializeRegularNodeKeepsCanTris() {
        RecordingPacketUartIO packetUartIO = new RecordingPacketUartIO();
        Node a = new Node(33, "testA", packetUartIO);
        a.addDevice(new TestDevice(3, 0, connectorMask(3)));
        a.initialize();

        // port B: B0, B1 outputs, B2 (CAN TX) output, B3 (CAN RX) input
        assertEquals(0b11111000, packetUartIO.getTris(33, 'B'));
        // port C: C4..C7 outputs
        assertEquals(0b00001111, packetUartIO.getTris(33, 'C'));
    }

    private static int connectorMask(int connectorNumber) {
        int mask = 0;
        for (Pin pin : AbstractConnectedDevice.layouts[connectorNumber - 1]) {
            mask |= pin.getBitMask() << (pin.getPort() - 'A') * 8;
        }
        return mask;
    }

    /**
     * Records sent packets and answers every request with an empty response.
     */
    private static class RecordingPacketUartIO extends PacketUartIOMock {
        final List<Packet> sent = new ArrayList<>();

        @Override
        public void send(Packet packet) {
            sent.add(packet);
        }

        @Override
        public Packet send(Packet packet, int responseType, int timeout) {
            sent.add(packet);
            return new Packet(packet.nodeId, responseType, null);
        }

        int getTris(int nodeId, char port) {
            Packet setPort = null;
            for (Packet p : sent) {
                if (p.nodeId == nodeId && p.messageType == MessageType.MSG_SetPortA + port - 'A') {
                    setPort = p;
                }
            }
            assertNotNull("No SetPort" + port + " sent", setPort);
            return setPort.data[3];
        }
    }

    private static class TestDevice implements ConnectedDevice {
        final int connectorNumber;
        final int eventMask;
        final int outputMask;

        private TestDevice(int connectorNumber, int eventMask, int outputMask) {
            this.connectorNumber = connectorNumber;
            this.eventMask = eventMask;
            this.outputMask = outputMask;
        }

        @Override
        public int getConnectorNumber() {
            return connectorNumber;
        }

        @Override
        public String getId() {
            return "42";
        }

        @Override
        public CpuFrequency getRequiredCpuFrequency() {
            return CpuFrequency.unknown;
        }

        @Override
        public int getEventMask() {
            return eventMask;
        }

        @Override
        public int getOutputMask() {
            return outputMask;
        }

        @Override
        public int getInitialOutputValues() {
            return 0;
        }
    }
}
