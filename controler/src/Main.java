import packet.PacketUartIO;
import packet.PacketUartIOException;
import packet.ReceivedPacketHandler;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            PacketUartIO packetUartIO = new PacketUartIO("COM1", 19200);
            packetUartIO.addReceivedPacketListener(new ReceivedPacketHandler());
            System.out.println("Listening ...");
            System.in.read();
            System.out.println("done.");
            packetUartIO.close();
        } catch (PacketUartIOException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
