import app.NodeInfoCollector;
import controller.ActionBinding;
import controller.device.OutputDevice;
import controller.device.WallSwitch;
import controller.actor.Actor;
import controller.actor.OnOffActor;
import node.Node;
import org.apache.log4j.Logger;
import packet.PacketUartIO;
import packet.PacketUartIOException;
import servlet.Servlet;

public class Main {
    static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            String port = (System.getenv("COMPUTERNAME") != null) ? "COM1" :  "/dev/ttyS80";
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

        WallSwitch obyvakA3 = new WallSwitch("obyvakA3", 11, 3);
        OutputDevice triacActor3 = new OutputDevice("triacActor3", 3, 2);


        OnOffActor svKoupelna = new OnOffActor("svKoupelna", triacActor3.getOut1(), 1, 0, false, obyvakA3.getGreenLed());
        OnOffActor svJidelna = new OnOffActor("svJidelna", triacActor3.getOut2(), 1, 0, true, obyvakA3.getRedLed());
        OnOffActor svSpajza =  new OnOffActor("svSpajza", triacActor3.getOut3(), 1, 0);
        OnOffActor svPradelna =  new OnOffActor("svPradelna", triacActor3.getOut4(), 1, 0);

        nodeInfoCollector.getSwitchListener().addActionBinding(new ActionBinding(obyvakA3.getButton1(), new Actor[]{svKoupelna}, null));
        nodeInfoCollector.getSwitchListener().addActionBinding(new ActionBinding(obyvakA3.getButton2(), new Actor[]{svJidelna}, null));
        nodeInfoCollector.getSwitchListener().addActionBinding(new ActionBinding(obyvakA3.getButton3(), new Actor[]{svSpajza}, null));
        nodeInfoCollector.getSwitchListener().addActionBinding(new ActionBinding(obyvakA3.getButton4(), new Actor[]{svPradelna}, null));


/*
        OnOffActor koupelna = new OnOffActor("koupelna", 3, Pin.pinC3, 1, 0, false, 11, Pin.pinC6);
        OnOffActor jidelna = new OnOffActor("jidelna", 3, Pin.pinC1, 1, 0, true, 11, Pin.pinC7);
        OnOffActor spajza =  new OnOffActor("spajza", 3, Pin.pinC0, 1, 0);
        OnOffActor pradelna =  new OnOffActor("pradelna", 3, Pin.pinA6, 1, 0);

        nodeInfoCollector.getSwitchListener().addActionBinding(new ActionBinding("obyvakASW31", 11, Pin.pinB0, new Actor[]{koupelna}, null));
        nodeInfoCollector.getSwitchListener().addActionBinding(new ActionBinding("obyvakASW32", 11, Pin.pinB1, new Actor[]{jidelna}, null));
        nodeInfoCollector.getSwitchListener().addActionBinding(new ActionBinding("obyvakASW33", 11, Pin.pinC5, new Actor[]{spajza}, null));
        nodeInfoCollector.getSwitchListener().addActionBinding(new ActionBinding("obyvakASW34", 11, Pin.pinC4, new Actor[]{pradelna}, null));

 */

    }
}
