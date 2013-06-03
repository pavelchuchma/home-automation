package packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PacketSerializer {
    List<Integer> buff = new ArrayList<Integer>();

    public void reset() {
        buff.clear();
    }

    public Packet readPacket(InputStream inputStream) throws IOException {
        Packet result = null;
        try {
            while (inputStream.available() > 0) {
                int b = inputStream.read();
                if (b < 0) throw new IOException("Unexpected end of stream");
                if (b < 128) {
                    if (buff.size() < 3) throw new IOException("CRC FAILED - too short packet: " + buff.size());
                    // last byte, check crc, return data
                    int eighthBits = buff.get(buff.size() - 1);
                    int crc = (buff.size() - 1) + eighthBits;
                    int[] packetData = new int[buff.size()-3];
                    for (int i=0; i<buff.size()-1; i++) {
                        if ((eighthBits & 1) != 0) buff.set(i, buff.get(i) | 128);
                        eighthBits = eighthBits >> 1;
                        crc += buff.get(i);
                        if (i>1) packetData[i-2] = (buff.get(i) & 0xFF);
                    }
                    // validate crc
                    if ((crc & 127) != b) throw new IOException("CRC Failed: " + (crc & 127) + " != " + b);
                    result = new Packet(buff.get(0), buff.get(1), packetData);
                    reset();
                    break;
                } else {
                    if (buff.size() > 7) throw new IOException("CRC FAILED - too long packet");
                    buff.add(b & 127);
                }

            }
        } catch (IOException e) {
            reset();
            throw e;
        }
        return result;
    }

    public void writePacket(Packet packet, OutputStream outputStream) throws IOException {
        byte[] buff = new byte[packet.length + 2];
        buff[0] = (byte) packet.nodeId;
        buff[1] = (byte) packet.messageType;
        if (packet.data != null) {
            for (int i=0; i<packet.data.length; i++) {
                buff[i+2] = (byte) packet.data[i];
            }
        }
        int eighthBits = 128;
        int crc = packet.length;
        for (int i=0; i<buff.length-2; i++) {
            if ((buff[i] & 128) != 0) eighthBits += (1 << i);
            buff[i] = (byte) (buff[i] | 128);
            crc += buff[i];
        }
        crc += eighthBits;
        buff[buff.length-2] = (byte) eighthBits;
        buff[buff.length-1] = (byte) (crc & 127);

        outputStream.write(buff);
    }

}