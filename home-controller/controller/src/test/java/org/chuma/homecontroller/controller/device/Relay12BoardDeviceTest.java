package org.chuma.homecontroller.controller.device;

import junit.framework.TestCase;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.packet.PacketUartIOMock;

public class Relay12BoardDeviceTest extends TestCase {
    public void testMapping() {
        Node n = new Node(33, "testA", new PacketUartIOMock());
        Relay12BoardDevice d = new Relay12BoardDevice("test", n);
        for (int i = 1; i <= 6; i++) {
            assertEquals("test-out" + i, d.getRelay(i).getName());
            assertEquals(AbstractConnectedDevice.getPin(1, i), d.getRelay(i).getPin());
        }
        for (int i = 7; i <= 12; i++) {
            assertEquals("test-out" + i, d.getRelay(i).getName());
            assertEquals(AbstractConnectedDevice.getPin(2, 13 - i), d.getRelay(i).getPin());
        }
    }

    public void testOutOfRange() {
        Relay12BoardDevice d = new Relay12BoardDevice("test", new Node(33, "testA", new PacketUartIOMock()));
        assertThrows(IllegalArgumentException.class, () -> d.getRelay(0));
        assertThrows(IllegalArgumentException.class, () -> d.getRelay(13));
    }

    private static void assertThrows(Class<? extends Throwable> expected, Runnable r) {
        try {
            r.run();
            fail("Expected " + expected.getSimpleName());
        } catch (Throwable t) {
            assertTrue(expected.isInstance(t));
        }
    }
}
