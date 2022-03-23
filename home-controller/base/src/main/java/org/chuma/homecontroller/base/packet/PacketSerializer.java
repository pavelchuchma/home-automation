package org.chuma.homecontroller.base.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serialize and deserialize packet (message) to/from the stream. The serialized message format is as follows:
 *
 * <ul>
 *   <li>byte - node ID
 *   <li>byte - message type
 *   <li>0..5 bytes - data
 *   <li>byte - highest bits byte
 *   <li>CRC
 * </ul>
 *
 * All bytes except CRC have highest bit set, CRC has highest bit cleared. So any byte with cleated highest
 * bit indicated last byte in message. The highest bits from message bytes are stored in "highest bits byte" -
 * the last but one byte of the message. Here the bit 0 is highest bit of the first byte (offset 0), bit 1
 * corresponds to second byte (offset 1), etc. CRC is calculated as sum of all bytes except CRC.
 */
public class PacketSerializer {
    static Logger log = LoggerFactory.getLogger(PacketSerializer.class.getName());
    static Logger msgLog = LoggerFactory.getLogger(PacketUartIO.class.getName() + ".msg");

    private List<Integer> buff = new ArrayList<Integer>();

    private void reset() {
        buff.clear();
    }

    private Packet readPacket(int b) throws IOException {
        if (b < 0) throw new IOException("Unexpected end of stream");
        if (b < 128) {
            if (buff.size() < 3) throw new IOException("CRC FAILED - too short packet: " + buff.size());
            // last byte, check crc, return data
            int eighthBits = buff.get(buff.size() - 1);
            int crc = (buff.size() - 1) + eighthBits;
            int[] packetData = new int[buff.size() - 3];
            for (int i = 0; i < buff.size() - 1; i++) {
                if ((eighthBits & 1) != 0) buff.set(i, buff.get(i) | 128);
                eighthBits = eighthBits >> 1;
                crc += buff.get(i);
                if (i > 1) packetData[i - 2] = (buff.get(i) & 0xFF);
            }
            // validate crc
            if ((crc & 127) != b) {
                StringBuilder sb = new StringBuilder();
                for (Integer i : buff) {
                    sb.append(i).append(", ");
                }
                throw new IOException(String.format("CRC FAILED: %d != %d (%d)-[%s]", crc & 127, b, buff.size(), sb));
            }
            Packet result = new Packet(buff.get(0), buff.get(1), packetData);
            reset();
            return result;
        } else {
            if (buff.size() > 7) throw new IOException("CRC FAILED - too long packet");
            buff.add(b & 127);
        }
        return null;
    }

    /**
     * Deserialize single packet from input stream.
     */
    public Packet readPacket(InputStream inputStream) throws IOException {
        try {
            while (true) {
                int b = inputStream.read();
                msgLog.trace(" > byte: " + b);
                if (b < 0) {
                    throw new IOException("End of stream reached");
                }
                Packet p = readPacket(b);
                if (p != null) {
                    return p;
                }
            }
        } catch (IOException e) {
            reset();
            throw e;
        }
    }

    /**
     * Serialize packet to output stream.
     */
    public static void writePacket(Packet packet, OutputStream outputStream) throws IOException {
        byte[] buff = new byte[packet.length + 2];
        buff[0] = (byte) packet.nodeId;
        buff[1] = (byte) packet.messageType;
        if (packet.data != null) {
            for (int i = 0; i < packet.data.length; i++) {
                buff[i + 2] = (byte) packet.data[i];
            }
        }
        int eighthBits = 128;
        int crc = packet.length;
        for (int i = 0; i < buff.length - 2; i++) {
            if ((buff[i] & 128) != 0) eighthBits += (1 << i);
            buff[i] = (byte) (buff[i] | 128);
            crc += buff[i];
        }
        crc += eighthBits;
        buff[buff.length - 2] = (byte) eighthBits;
        buff[buff.length - 1] = (byte) (crc & 127);

        outputStream.write(buff);
        outputStream.flush();
    }
}