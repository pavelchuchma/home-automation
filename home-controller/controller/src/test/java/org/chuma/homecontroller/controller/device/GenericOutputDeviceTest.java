package org.chuma.homecontroller.controller.device;

import junit.framework.TestCase;
import org.junit.Assert;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.packet.PacketUartIOMock;

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
}