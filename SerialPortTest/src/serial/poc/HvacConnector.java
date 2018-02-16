package serial.poc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class HvacConnector implements IPacketSource {
    public static final int MAX_PACKET_BYTE_READ_TIME = 10;
    private final OutputStream outputStream;
    BlockingQueue<PacketData> packetDataQueue = new LinkedBlockingQueue<>();
    boolean closed = false;
    int[] readBuff = new int[256];
    InputStream inputStream;
    ByteLogger byteLogger;
    ConcurrentLinkedQueue<PacketData> sendQueue = new ConcurrentLinkedQueue<>();
    private int counter = 0;

    public HvacConnector(InputStream inputStream, OutputStream outputStream) throws IOException {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        byteLogger = new ByteLogger();
    }

    public void sendData(PacketData p) throws IOException {
        sendQueue.add(p);
    }

    public void sendDataImpl(PacketData p) throws IOException {
        try {
            Thread.sleep(30);
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
            System.out.println("Starting read com thread");

            ReceivedChar c = null;
            while (!closed) {
                try {
                    if (c == null) {
                        c = readChar();
                    }
                    while (!closed) {
                        while (c.readTime <= MAX_PACKET_BYTE_READ_TIME) {
                            System.out.printf("ERR: ignoring initial char %d with read time: %d%n", c.character, c.readTime);
                            c = readChar();
                        }

                        int i = 0;
                        int initialReadTime = 0;
                        while (!closed) {
                            if ((c.readTime > MAX_PACKET_BYTE_READ_TIME || (i >= 14 && c.character == PacketData.START_BYTE))
                                    && i > 0 && readBuff[i - 1] == PacketData.STOP_BYTE) {
                                PacketData packetData = new PacketData(Arrays.copyOf(readBuff, i), initialReadTime);
                                packetDataQueue.add(packetData);
                                i = 0;
                            }
                            if (i + 1 >= readBuff.length) {
                                System.out.println("ERR: Not enough space in readBuff");
                                break;
                            }
                            if (i == 0) {
                                initialReadTime = c.readTime;
                            }
                            readBuff[i++] = c.character;

                            if (c.character == PacketData.STOP_BYTE) {
                                if (i == 14 && !Main.isWindows()) {
                                    if (readBuff[3] == 0xD1) {
                                        PacketData packet = sendQueue.poll();
                                        if (packet != null) {
                                            sendDataImpl(packet);
                                        }
                                        counter++;
                                    }
                                }
                                byteLogger.flush();
                            }
                            // read next char
                            c = readChar();
                        }
                    }
                } catch (IOException e) {
                    System.out.println("EXCEPTION:" + e);
                    e.printStackTrace();
                }

            }
        }, "ComRead").start();
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
