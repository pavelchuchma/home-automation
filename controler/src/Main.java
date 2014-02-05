import app.NodeInfoCollector;
import controller.ActionBinding;
import controller.actor.Actor;
import controller.actor.OnOffActor;
import controller.device.OutputDevice;
import controller.device.WallSwitch;
import node.Node;
import org.apache.log4j.Logger;
import packet.PacketUartIO;
import packet.PacketUartIOException;
import servlet.Servlet;

public class Main {
    static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            String port = (System.getenv("COMPUTERNAME") != null) ? "COM1" : "/dev/ttyS80";
            PacketUartIO packetUartIO = new PacketUartIO(port, 19200);
            NodeInfoCollector nodeInfoCollector = new NodeInfoCollector(packetUartIO);

            configure(nodeInfoCollector);

            nodeInfoCollector.start();
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

    private static void configure(NodeInfoCollector nodeInfoCollector) {

        Node brige = nodeInfoCollector.createNode(1, "Bridge");
        Node triak1 = nodeInfoCollector.createNode(3, "Triak1");
        Node obyvakSpinacABC = nodeInfoCollector.createNode(11, "ObyvakSpinacABC");

        WallSwitch obyvakA3sw = new WallSwitch("obyvakA3sw", obyvakSpinacABC, 3);
        OutputDevice triak1Actor2 = new OutputDevice("triak1Actor2", triak1, 2);

        OnOffActor svKoupelna = new OnOffActor("svKoupelna", triak1Actor2.getOut1(), 1, 0, false, obyvakA3sw.getGreenLed());
        OnOffActor svJidelna = new OnOffActor("svJidelna", triak1Actor2.getOut2(), 1, 0, true, obyvakA3sw.getRedLed());
        OnOffActor svSpajza = new OnOffActor("svSpajza", triak1Actor2.getOut3(), 1, 0);
        OnOffActor svPradelna = new OnOffActor("svPradelna", triak1Actor2.getOut4(), 1, 0);

        nodeInfoCollector.getSwitchListener().addActionBinding(new ActionBinding(obyvakA3sw.getButton1(), new Actor[]{svKoupelna}, null));
        nodeInfoCollector.getSwitchListener().addActionBinding(new ActionBinding(obyvakA3sw.getButton2(), new Actor[]{svJidelna}, null));
        nodeInfoCollector.getSwitchListener().addActionBinding(new ActionBinding(obyvakA3sw.getButton3(), new Actor[]{svSpajza}, null));
        nodeInfoCollector.getSwitchListener().addActionBinding(new ActionBinding(obyvakA3sw.getButton4(), new Actor[]{svPradelna}, null));

//        triak1.initialize();
//        obyvakSpinacABC.initialize();

    }
}
