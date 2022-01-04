package org.chuma.homecontroller.nodes.packet;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class PacketSerializerTest {

    byte[] inputData = new byte[]{
            (byte) 138, (byte) 200, (byte) 255, (byte) 128, (byte) 255, (byte) 154, 111, // #0
            (byte) 153, (byte) 161, (byte) 228, (byte) 128, 33,                         // #1
            (byte) 153, (byte) 161, (byte) 228, (byte) 229, (byte) 130, (byte) 255, (byte) 253, (byte) 176, 56, // #2
            (byte) 153, (byte) 161, (byte) 228, (byte) 128, 44,                         // #3 bad CRC
            (byte) 153, (byte) 161, (byte) 228, (byte) 128, (byte) 144, (byte) 223, (byte) 135, (byte) 145, (byte) 222, 100 // #4 too long, bad CRC
    };

    Packet exp1 = new Packet(10,200,new int[]{127, 128, 255});
    Packet exp2 = new Packet(25,33,new int[]{100});
    Packet exp3 = new Packet(25,33,new int[]{100,101, 130, 255, 125});

    @Test
    public void testReadPacket() throws Exception {

        ByteArrayInputStream completeStream = new ByteArrayInputStream(inputData);
        PacketSerializer packetSerializer = new PacketSerializer();

        Packet p = packetSerializer.readPacket(completeStream);
        Assert.assertEquals("First packet", exp1, p);

        p = packetSerializer.readPacket(completeStream);
        Assert.assertEquals("First packet", exp2, p);

        p = packetSerializer.readPacket(completeStream);
        Assert.assertEquals("First packet", exp3, p);

        try {
            // #3 bad CRC
            packetSerializer.readPacket(completeStream);
            Assert.fail();
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().startsWith("CRC Failed: "));
        }

        try {
            // #4 too long, bad CRC
            packetSerializer.readPacket(completeStream);
            Assert.fail();
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().startsWith("CRC FAILED - too long packet"));
        }
    }

    @Test
    public void testReadPacketByParts() throws Exception {
        PacketSerializer packetSerializer = new PacketSerializer();
        Packet p;
        p = packetSerializer.readPacket(new ByteArrayInputStream(inputData, 0, 2));
        Assert.assertNull(p);
        p = packetSerializer.readPacket(new ByteArrayInputStream(inputData, 2, 3));
        Assert.assertNull(p);
        InputStream is = new ByteArrayInputStream(inputData, 5, 4);
        p = packetSerializer.readPacket(is);
        Assert.assertEquals(exp1, p); //first finished, 2 remains unread

        p = packetSerializer.readPacket(is);
        Assert.assertNull(p);
        is = new ByteArrayInputStream(inputData, 9, 1);
        p = packetSerializer.readPacket(is);
        is = new ByteArrayInputStream(inputData, 10, 2);
        p = packetSerializer.readPacket(is);
        Assert.assertEquals(exp2, p); //first finished, 0 remains unread
    }

    @Test
    public void testWritePacket() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        PacketSerializer packetSerializer = new PacketSerializer();
        packetSerializer.writePacket(exp1, os);
        packetSerializer.writePacket(exp2, os);
        packetSerializer.writePacket(exp3, os);

        byte[] writtenBytes = os.toByteArray();
        Assert.assertArrayEquals(Arrays.copyOfRange(inputData, 0, 21), writtenBytes);
   }
}