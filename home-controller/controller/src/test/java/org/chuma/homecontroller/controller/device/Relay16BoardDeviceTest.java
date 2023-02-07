package org.chuma.homecontroller.controller.device;

import java.io.IOException;

import junit.framework.TestCase;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.Pin;
import org.chuma.homecontroller.base.packet.PacketUartIOMock;
import org.chuma.homecontroller.base.packet.simulation.SimulatedNode;
import org.chuma.homecontroller.base.packet.simulation.SimulatedPacketUartIO;
import org.chuma.homecontroller.controller.actor.OnOffActor;

public class Relay16BoardDeviceTest extends TestCase {
    public void testPins() {
        Node n = new Node(33, "testA", new PacketUartIOMock());
        Relay16BoardDevice d = new Relay16BoardDevice("test", n);
        for (int i = 1; i <= 16; i++) {
            System.out.println("#" + i + ": =" + d.getRelay(i).getName() + "; " + d.getRelay(i).toString());
        }
    }

    public void testSwitching() throws IOException {
        try (SimulatedPacketUartIO sim = new SimulatedPacketUartIO()) {
            sim.start();
            Node n = new Node(-1, "testA", sim);
            sim.registerNode(n);
            Relay16BoardDevice d = new Relay16BoardDevice("test", n);
            SimulatedNode sn = sim.getSimulatedNode(n.getNodeId());
            n.initialize();

            OnOffActor actor11 = new OnOffActor("a11", "", d.getRelay(11));

            ExpectedPortsState exp = new ExpectedPortsState(sn);

            Pin[] allOutputs = {
                    Pin.pinA5, Pin.pinA3, Pin.pinA2, Pin.pinA0, Pin.pinB4, Pin.pinB5,
                    Pin.pinC3, Pin.pinC1, Pin.pinC0, Pin.pinA6, Pin.pinC2, Pin.pinA7,
                    Pin.pinB0, Pin.pinB1, Pin.pinC5, Pin.pinC4, Pin.pinC6, Pin.pinC7
            };

            exp.setTrisZeroes(allOutputs);
            exp.setPortOnes(allOutputs);
            exp.assetState();

            actor11.switchOn();
            exp.setPortZeroes(Pin.pinA7);
            exp.assetState();
        }
    }
}