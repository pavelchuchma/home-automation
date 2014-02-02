import app.NodeInfoCollector;
import node.Node;
import org.apache.log4j.Logger;
import packet.PacketUartIO;
import packet.PacketUartIOException;
import servlet.Servlet;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class Main {
    static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            PacketUartIO packetUartIO = new PacketUartIO("/dev/ttyS80", 19200);
            //PacketUartIO packetUartIO = new PacketUartIO("COM1", 19200);
            NodeInfoCollector nodeInfoCollector = new NodeInfoCollector(packetUartIO);

            System.out.println("Listening ...");
            int val = 0;


            //testParallelGetBuildTime(nodeInfoCollector);

            Node node3 = nodeInfoCollector.getNode(3);
/*
            try {
//                node3.setPortValue('A', Bits.bit7, 0xFF);
//                node3.setPortValue('C', Bits.bit3, 0xFF);
//                node3.setPortValue('C', Bits.bit1, 0xFF);
//                node3.setPortValue('C', Bits.bit0, 0xFF);
//                node3.setPortValue('A', Bits.bit6, 0xFF);
//                node3.setPortValue('C', Bits.bit2, 0xFF);

                //node3.setHeartBeatPeriod(1);
//                node3.setPortValue('C', Bits.bit3, 0x00);
//                node3.setPortValue('C', Bits.bit1, 0x00);
//                node3.setPortValue('C', Bits.bit0, 0x00);
//                node3.setPortValue('A', Bits.bit6, 0x00);
//                node3.setPortValue('C', Bits.bit2, 0x00);
//                node3.setPortValue('A', Bits.bit7, 0x00);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IllegalArgumentException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }*/

            Servlet.startServer(nodeInfoCollector);

            while (true) {
                Thread.sleep(1000);
                node3 = nodeInfoCollector.getNode(3);
                if (node3 != null) {
//                    try {
//
////                        switch (val % 6) {
////                            case 0:
////                                node3.setPortValue('A', Bits.bit7, 0xFF);
////                                node3.setPortValue('C', Bits.bit3, 0x00);
////                                break;
////                            case 1:
////                                node3.setPortValue('C', Bits.bit3, 0xFF);
////                                node3.setPortValue('C', Bits.bit1, 0x00);
////                                break;
////                            case 2:
////                                node3.setPortValue('C', Bits.bit1, 0xFF);
////                                node3.setPortValue('C', Bits.bit0, 0x00);
////                                break;
////                            case 3:
////                                node3.setPortValue('C', Bits.bit0, 0xFF);
////                                node3.setPortValue('A', Bits.bit6, 0x00);
////                                break;
////                            case 4:
////                                node3.setPortValue('A', Bits.bit6, 0xFF);
////                                node3.setPortValue('C', Bits.bit2, 0x00);
////                                break;
////                            case 5:
////                                node3.setPortValue('C', Bits.bit2, 0xFF);
////                                node3.setPortValue('A', Bits.bit7, 0x00);
////                                break;
////                        }
//                        val++;
//                    } catch (IOException e) {
//                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//                    } catch (IllegalArgumentException e) {
//                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//                    }
                }
            }
        } catch (PacketUartIOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
