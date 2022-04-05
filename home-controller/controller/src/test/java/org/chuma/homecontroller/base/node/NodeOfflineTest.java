package org.chuma.homecontroller.base.node;

import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.rules.ExpectedException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.chuma.homecontroller.base.packet.PacketUartIOMock;

public class NodeOfflineTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

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
        public CpuFrequency getRequiredCpuFrequency() {
            return CpuFrequency.unknown;
        }

        @Override
        public int getEventMask() {
            return eventMask;
        }

        @Override
        public int getOutputMasks() {
            return outputMask;
        }

        @Override
        public int getInitialOutputValues() {
            return 0;
        }
    }
}
