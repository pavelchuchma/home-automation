package serial.poc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class StreamPacketSource implements IPacketSource {
    BlockingQueue<PacketData> packetDataQueue = new LinkedBlockingQueue<>();
    boolean closed = false;
    int[] readBuff = new int[256];
    InputStream inputStream;

    public StreamPacketSource(InputStream inputStream) {
        this.inputStream = inputStream;
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
                        while (c.readTime <= 10) {
                            System.out.printf("ERR: ignoring initial char %d with read time: %d%n", c.character, c.readTime);
                            c = readChar();
                        }

                        int i = 0;
                        int initialReadTime = 0;
                        while (!closed) {
                            if (c.readTime > 10 && i > 0 && readBuff[i - 1] == 52) {
                                packetDataQueue.add(new PacketData(Arrays.copyOf(readBuff, i), initialReadTime));
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
        return new ReceivedChar(b, (int) (System.currentTimeMillis() - startTime));
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
