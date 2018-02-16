package serial.poc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class HvacConnector implements IPacketSource {
    public static final int MAX_PACKET_BYTE_READ_TIME = 10;
    private final OutputStream outputStream;
    BlockingQueue<PacketData> packetDataQueue = new LinkedBlockingQueue<>();
    boolean closed = false;
    InputStream inputStream;
    ByteLogger byteLogger;
    ConcurrentLinkedQueue<PacketData> sendQueue = new ConcurrentLinkedQueue<>();

    public HvacConnector(InputStream inputStream, OutputStream outputStream) throws IOException {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        byteLogger = new ByteLogger();
    }

    public void sendData(PacketData p) throws IOException {
        sendQueue.add(p);
    }

    private void sendDataImpl(PacketData p) throws IOException {
        try {
            Thread.sleep(20);
            for (int c : p.rawData) {
                outputStream.write(new byte[]{(byte) c});
                outputStream.flush();
                byteLogger.byteSent(c);
                Thread.sleep(5);
            }
            System.out.println("sent!!  ");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void startRead() {
        new Thread(() -> {
            ReceivedChar c = null;
            int[] buff = new int[PacketData.PACKET_LENGTH];

            while (!closed) {
                try {
                    c = waitForStartChar(c);
                    int initialReadTime = c.readTime;
                    for (int i = 0; i < PacketData.PACKET_LENGTH - 1 && !closed; i++) {
                        buff[i] = c.character;
                        c = readChar();
                    }
                    buff[PacketData.PACKET_LENGTH - 1] = c.character;

                    if (buff[PacketData.PACKET_LENGTH - 1] != PacketData.STOP_BYTE) {
                        throw new IOException("Read packet not terminated by STOP_BYTE");
                    }

                    if (buff[3] == 0xD1) {
                        PacketData packet = sendQueue.poll();
                        if (packet != null) {
                            sendDataImpl(packet);
                        }
                    }

                    PacketData packetData = new PacketData(buff, initialReadTime);
                    packetDataQueue.add(packetData);

                    byteLogger.flush();
                } catch (IOException e) {
                    System.out.println("EXCEPTION:" + e);
                    e.printStackTrace();
                }
            }
        }, "HvacRead").start();
    }

    private ReceivedChar waitForStartChar(ReceivedChar c) throws IOException {
        if (c == null || c.character == PacketData.STOP_BYTE) {
            c = readChar();
            if (c.character == PacketData.START_BYTE) {
                return c;
            }
        }
        // clear read cache and wait for start char
        while (c.character != PacketData.START_BYTE || c.readTime <= MAX_PACKET_BYTE_READ_TIME) {
            System.out.printf("ERR: ignoring initial char %02X with read time: %d%n", c.character, c.readTime);
            c = readChar();
        }
        return c;
    }

    @Override
    public PacketData getPacket() throws InterruptedException {
        return packetDataQueue.take();
    }

    private ReceivedChar readChar() throws IOException {
        long startTime = System.currentTimeMillis();
        int b = inputStream.read();
        if (b < 0) {
            throw new IOException("End of stream reached");
        }
        int readTime = (int) (System.currentTimeMillis() - startTime);
        byteLogger.logByte(readTime, b);
        return new ReceivedChar(b, readTime);
    }

    static class ReceivedChar {
        int character;
        int readTime;

        public ReceivedChar(int character, int readTime) {
            this.character = character;
            this.readTime = readTime;
        }
    }
}
