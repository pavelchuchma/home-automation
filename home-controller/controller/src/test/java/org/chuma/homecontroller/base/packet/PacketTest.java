package org.chuma.homecontroller.base.packet;

import org.chuma.homecontroller.base.node.MessageType;
import org.junit.Test;

public class PacketTest {
    @Test
    public void testCreation() {
        try {
            new Packet(2, MessageType.MSG_SetPortA, new int[]{1, 2, 3, 4, 5, 6});
        } catch (IllegalArgumentException e) {
        }
        Packet p = new Packet(2, MessageType.MSG_SetPortA, new int[]{1, 2, 3, 4, 5});


        System.out.println(p.toString());
    }
}