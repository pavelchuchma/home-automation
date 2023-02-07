package org.chuma.homecontroller.controller.device;

import java.io.IOException;

import junit.framework.TestCase;
import org.junit.Assert;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.Pin;
import org.chuma.homecontroller.base.packet.PacketUartIOMock;
import org.chuma.homecontroller.base.packet.simulation.SimulatedNode;
import org.chuma.homecontroller.base.packet.simulation.SimulatedPacketUartIO;
import org.chuma.homecontroller.controller.actor.OnOffActor;

public class GenericOutputDeviceTest extends TestCase {
    public void testInit() {
        {
            Node n = new Node(33, "testA", new PacketUartIOMock());
            GenericOutputDevice d = new GenericOutputDevice("gen1", n, 1, true);
            Assert.assertEquals(0b0011_1111, d.getDevicePinOutputMask());
            Assert.assertEquals(0b0000_0000__0011_0000__0010_1101, d.getOutputMask());
            Assert.assertEquals(0b0000_0000__0000_0000__0000_0000, d.getInitialOutputValues());
        }
        {
            Node n = new Node(33, "testA", new PacketUartIOMock());
            GenericOutputDevice d = new GenericOutputDevice("gen1", n, 2, false);
            Assert.assertEquals(0b0011_1111, d.getDevicePinOutputMask());
            Assert.assertEquals(0b0000_1111__0000_0000__1100_0000, d.getOutputMask());
            Assert.assertEquals(d.getInitialOutputValues(), d.getInitialOutputValues());
        }
        {
            Node n = new Node(33, "testA", new PacketUartIOMock());
            GenericOutputDevice d = new GenericOutputDevice("gen1", n, 3, false);
            Assert.assertEquals(0b0011_1111, d.getDevicePinOutputMask());
            Assert.assertEquals(0b1111_0000__0000_0011__0000_0000, d.getOutputMask());
            Assert.assertEquals(d.getInitialOutputValues(), d.getInitialOutputValues());
        }
        {
            Node n = new Node(33, "testA", new PacketUartIOMock());
            GenericOutputDevice d = new GenericOutputDevice("gen1", n, 1, false);
            Assert.assertEquals(0b0011_1111, d.getDevicePinOutputMask());
            Assert.assertEquals(0b0000_0000__0011_0000__0010_1101, d.getOutputMask());
            Assert.assertEquals(d.getInitialOutputValues(), d.getInitialOutputValues());
        }
    }

    public void testTestWithSimulatedNode() throws IOException {
        try (SimulatedPacketUartIO sim = new SimulatedPacketUartIO()) {
            sim.start();
            Node n = new Node(-1, "testA", sim);
            sim.registerNode(n);
            GenericOutputDevice d1 = new GenericOutputDevice("gen1", n, 1, true);
            GenericOutputDevice d2 = new GenericOutputDevice("gen2", n, 2, false);
            OnOffActor actor11 = new OnOffActor("a11", "", d1.getOut1());
            OnOffActor actor16 = new OnOffActor("a16", "", d1.getOut6());
            OnOffActor actor23 = new OnOffActor("a23", "", d2.getOut3());
            SimulatedNode sn = sim.getSimulatedNode(n.getNodeId());
            n.initialize();

            ExpectedPortsState exp = new ExpectedPortsState(sn);

            exp.setTrisZeroes(Pin.pinA5, Pin.pinA3, Pin.pinA2, Pin.pinA0, Pin.pinB4, Pin.pinB5);
            exp.setTrisZeroes(Pin.pinC3, Pin.pinC1, Pin.pinC0, Pin.pinA6, Pin.pinC2, Pin.pinA7);
            exp.setPortOnes(Pin.pinC3, Pin.pinC1, Pin.pinC0, Pin.pinA6, Pin.pinC2, Pin.pinA7);
            exp.assetState();
            actor11.switchOff();
            exp.assetState();

            actor11.switchOn();
            exp.setPortOnes(Pin.pinA5);
            exp.assetState();

            actor23.switchOn();
            exp.setPortZeroes(Pin.pinC0);
            exp.assetState();

            actor16.switchOn();
            exp.setPortOnes(Pin.pinB5);
            exp.assetState();

            actor11.switchOff();
            exp.setPortZeroes(Pin.pinA5);
            exp.assetState();

            actor23.switchOff();
            exp.setPortOnes(Pin.pinC0);
            exp.assetState();
        }
    }

    public void testTriacBoardDevice() throws IOException {
        try (SimulatedPacketUartIO sim = new SimulatedPacketUartIO()) {
            sim.start();
            Node n = new Node(-1, "testA", sim);
            sim.registerNode(n);
            TriacBoardDevice t3 = new TriacBoardDevice("tri3", n, 3);
            OnOffActor actor32 = new OnOffActor("a32", "", t3.getOut2());
            SimulatedNode sn = sim.getSimulatedNode(n.getNodeId());
            n.initialize();

            ExpectedPortsState exp = new ExpectedPortsState(sn);

            exp.setTrisZeroes(Pin.pinB0, Pin.pinB1, Pin.pinC5, Pin.pinC4, Pin.pinC6, Pin.pinC7);
            exp.setPortOnes(Pin.pinB0, Pin.pinB1, Pin.pinC5, Pin.pinC4, Pin.pinC6, Pin.pinC7);
            exp.assetState();

            actor32.switchOff();
            exp.assetState();

            exp.printDebug();
            actor32.switchOn();
            exp.printDebug();
            exp.setPortZeroes(Pin.pinB1);
            exp.assetState();

            actor32.switchOff();
            exp.setPortOnes(Pin.pinB1);
            exp.assetState();
        }
    }
}