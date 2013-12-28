import app.NodeInfoCollector;
import packet.Packet;
import packet.PacketUartIO;
import packet.PacketUartIOException;
import packet.ReceivedPacketHandler;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class Main {
    public static void main(String[] args) {
        try {
            PacketUartIO packetUartIO = new PacketUartIO("COM1", 19200);
            NodeInfoCollector nodeInfoCollector = new NodeInfoCollector(packetUartIO);

            System.out.println("Listening ...");
            for (int i = 0; i<200; i++) {
                Thread.sleep(1000);
                PrintStream out = null;
                try {
                    out = new PrintStream(new FileOutputStream("out\\report.html"));
                    out.print(nodeInfoCollector.getReport());
                } finally {
                    if (out != null) out.close();
                }
            }

            packetUartIO.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (PacketUartIOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
