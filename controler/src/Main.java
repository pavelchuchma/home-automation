import app.NodeInfoCollector;
import com.sun.javaws.exceptions.InvalidArgumentException;
import node.Bits;
import node.Node;
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
            int val = 0;
            while (true) {
                Thread.sleep(1000);
                Node node3 = nodeInfoCollector.getNode(3);
                if (node3 != null) {
                    try {
                        if (false) node3.setPortValue('C', Bits.bit1, val);
                        val ^= 0xFF;
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    } catch (InvalidArgumentException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }

                PrintStream out = null;
                try {
                    out = new PrintStream(new FileOutputStream("out\\report.html"));
                    out.print(nodeInfoCollector.getReport());
                } finally {
                    if (out != null) out.close();
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (PacketUartIOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
