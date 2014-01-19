import app.NodeInfoCollector;
import com.sun.javaws.exceptions.InvalidArgumentException;
import node.Bits;
import node.Node;
import org.apache.log4j.Logger;
import packet.PacketUartIO;
import packet.PacketUartIOException;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class Main {
    static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            PacketUartIO packetUartIO = new PacketUartIO("COM1", 19200);
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
            } catch (InvalidArgumentException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }*/


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
//                    } catch (InvalidArgumentException e) {
//                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//                    }
                }
                PrintStream out = null;
                try {
                    out = new PrintStream(new FileOutputStream("out\\report.html"));
                    out.print(nodeInfoCollector.getReport());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } finally {
                    if (out != null) out.close();
                }
            }
        } catch (PacketUartIOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private static void testParallelGetBuildTime(NodeInfoCollector nodeInfoCollector) {
        final Node node3 = nodeInfoCollector.getNode(3);
        final Node node6 = nodeInfoCollector.getNode(6);
        final Node node12 = nodeInfoCollector.getNode(12);

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 200; i++) {
                    try {
                        if (node3.getBuildTime() == null) {
                            log.error("!!!No buildtime for node3!!!");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 200; i++) {
                    try {
                        if (node6.getBuildTime() == null) {
                            log.error("!!!No buildtime for node6!!!");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 200; i++) {
                    try {
                        if (node12.getBuildTime() == null) {
                            log.error("!!!No buildtime for node12!!!");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }
        }).start();
    }
}
