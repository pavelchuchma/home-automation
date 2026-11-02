package org.chuma.homecontroller.controller.device;

import java.io.IOException;

import junit.framework.TestCase;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.Pin;
import org.chuma.homecontroller.base.packet.PacketUartIOMock;
import org.chuma.homecontroller.base.packet.simulation.SimulatedNode;
import org.chuma.homecontroller.base.packet.simulation.SimulatedPacketUartIO;
import org.chuma.homecontroller.controller.actor.OnOffActor;

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

    public void testPolarity() {
        Relay12BoardDevice d = new Relay12BoardDevice("test", new Node(33, "testA", new PacketUartIOMock()));
        for (int i = 1; i <= 12; i++) {
            assertEquals("relay " + i, i <= 4, d.getRelay(i).isHighValueMeansOn());
        }
    }

    public void testWithSimulatedNode() throws IOException {
        try (SimulatedPacketUartIO sim = new SimulatedPacketUartIO()) {
            sim.start();
            Node n = new Node(-1, "testA", sim);
            sim.registerNode(n);
            Relay12BoardDevice d = new Relay12BoardDevice("test", n);
            OnOffActor relay1 = new OnOffActor("r1", "", d.getRelay(1));
            OnOffActor relay5 = new OnOffActor("r5", "", d.getRelay(5));
            OnOffActor relay7 = new OnOffActor("r7", "", d.getRelay(7));
            SimulatedNode sn = sim.getSimulatedNode(n.getNodeId());
            n.initialize();

            ExpectedPortsState exp = new ExpectedPortsState(sn);
            exp.setTrisZeroes(Pin.pinA5, Pin.pinA3, Pin.pinA2, Pin.pinA0, Pin.pinB4, Pin.pinB5);
            exp.setTrisZeroes(Pin.pinC3, Pin.pinC1, Pin.pinC0, Pin.pinA6, Pin.pinC2, Pin.pinA7);
            // all relays off: high-triggered relays 1-4 at 0, low-triggered relays 5-12 at 1
            exp.setPortOnes(Pin.pinB4, Pin.pinB5);
            exp.setPortOnes(Pin.pinC3, Pin.pinC1, Pin.pinC0, Pin.pinA6, Pin.pinC2, Pin.pinA7);
            exp.assetState();

            relay1.switchOn();
            exp.setPortOnes(Pin.pinA5);
            exp.assetState();

            relay5.switchOn();
            exp.setPortZeroes(Pin.pinB4);
            exp.assetState();

            relay7.switchOn();
            exp.setPortZeroes(Pin.pinA7);
            exp.assetState();

            relay1.switchOff();
            exp.setPortZeroes(Pin.pinA5);
            exp.assetState();
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
