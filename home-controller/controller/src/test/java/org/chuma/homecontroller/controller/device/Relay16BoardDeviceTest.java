package org.chuma.homecontroller.controller.device;

import junit.framework.TestCase;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.packet.PacketUartIOMock;

public class Relay16BoardDeviceTest extends TestCase {
    public void testPins() {
        Node n = new Node(33, "testA", new PacketUartIOMock());
        Relay16BoardDevice d = new Relay16BoardDevice("test", n);
        for (int i = 1; i<=16;i++) {
            System.out.println("#" + i + ": =" + d.getPin(i).getName() + "; " + d.getPin(i).toString());
        }
    }
}