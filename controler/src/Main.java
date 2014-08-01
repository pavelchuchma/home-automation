import app.NodeInfoCollector;
import app.SwitchListener;
import controller.Action.*;
import controller.ActionBinding;
import controller.actor.OnOffActor;
import controller.device.InputDevice;
import controller.device.OutputDevice;
import controller.device.RelayBoardDevice;
import controller.device.WallSwitch;
import node.CpuFrequency;
import node.Node;
import org.apache.log4j.Logger;
import packet.IPacketUartIO;
import packet.PacketUartIO;
import packet.PacketUartIOException;
import packet.PacketUartIOMock;
import servlet.Servlet;

import java.io.IOException;

public class Main {
    static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            String port = (System.getenv("COMPUTERNAME") != null) ? "COM1" : "/dev/ttyS80";
            IPacketUartIO packetUartIO;
            try {
                packetUartIO = new PacketUartIO(port, 19200);
            } catch (PacketUartIOException e) {
                if (System.getenv("COMPUTERNAME") != null) {
                    packetUartIO = new PacketUartIOMock();
                } else {
                    throw e;
                }
            }

            NodeInfoCollector nodeInfoCollector = new NodeInfoCollector(packetUartIO);

            configure(nodeInfoCollector);

            nodeInfoCollector.start();
            System.out.println("Listening ...");

            //testParallelGetBuildTime(nodeInfoCollector);

            Servlet.startServer(nodeInfoCollector);

        } catch (PacketUartIOException e) {
            log.error("Initialization failed", e);
            e.printStackTrace();
            System.exit(1);
        } catch (InterruptedException e) {
            log.error("Initialization failed", e);
            System.exit(2);
        } catch (Exception e) {
            log.error("Initialization failed", e);
            System.exit(3);
        }
    }

    private static void configure(NodeInfoCollector nodeInfoCollector) {

        Node bridge = nodeInfoCollector.createNode(1, "Bridge");
        Node actor3 = nodeInfoCollector.createNode(3, "Actor3");
        Node zaluzieA = nodeInfoCollector.createNode(13, "ZaluzieA");
        Node obyvakSpinacABC = nodeInfoCollector.createNode(11, "ObyvakSpinacABC");
        Node chodbaDole = nodeInfoCollector.createNode(8, "ChodbaDole");
        Node koupelnaHore = nodeInfoCollector.createNode(9, "KoupelnaHore");

        WallSwitch obyvakA3sw = new WallSwitch("obyvakA3sw", obyvakSpinacABC, 3);
        OutputDevice triak1Actor3Port3 = new OutputDevice("triak1Actor3Port3", actor3, 3);
        RelayBoardDevice rele1Actor3Port2 = new RelayBoardDevice("rele1Actor3Port2", actor3, 2);

        WallSwitch koupelnaHoreSwA = new WallSwitch("koupelnaHoreSwA", koupelnaHore, 1);
        WallSwitch koupelnaHoreSwB = new WallSwitch("koupelnaHoreSwB", koupelnaHore, 2);
        WallSwitch chodbaDoldeSwA = new WallSwitch("chodbaDoldeSwA", chodbaDole, 1);

        OnOffActor svKoupelna = new OnOffActor("svKoupelna", triak1Actor3Port3.getOut1(), 1, 0, obyvakA3sw.getGreenLedIndicator(false), koupelnaHoreSwA.getGreenLedIndicator(false), koupelnaHoreSwA.getRedLedIndicator(true));
        OnOffActor svJidelna = new OnOffActor("svJidelna", triak1Actor3Port3.getOut2(), 1, 0, obyvakA3sw.getRedLedIndicator(true));
        OnOffActor svSpajza = new OnOffActor("svSpajza", triak1Actor3Port3.getOut3(), 1, 0);
        OnOffActor svPradelna = new OnOffActor("svPradelna", triak1Actor3Port3.getOut4(), 1, 0, chodbaDoldeSwA.getGreenLedIndicator(false), chodbaDoldeSwA.getRedLedIndicator(true));
        OnOffActor zaricKoupelnaHore2Trubice = new OnOffActor("zaricKoupelnaHore2Trubice", rele1Actor3Port2.getRele1(), 0, 1, koupelnaHoreSwB.getRedLedIndicator(true));
        OnOffActor zaricKoupelnaHore1Trubice = new OnOffActor("zaricKoupelnaHore1Trubice", rele1Actor3Port2.getRele2(), 0, 1, koupelnaHoreSwB.getGreenLedIndicator(true));

        SwitchListener lst = nodeInfoCollector.getSwitchListener();

        Action invertJidelna = new InvertAction(svJidelna);
        lst.addActionBinding(new ActionBinding(obyvakA3sw.getButton1(), new Action[]{new InvertAction(svKoupelna)}, null));
        lst.addActionBinding(new ActionBinding(obyvakA3sw.getButton2(), new Action[]{invertJidelna}, null));
        //lst.addActionBinding(new ActionBinding(obyvakA3sw.getButton3(), new Action[]{new InvertAction(svSpajza)}, null));
        //lst.addActionBinding(new ActionBinding(obyvakA3sw.getButton4(), new Action[]{new InvertAction(svPradelna)}, null));

        Action onActionKoupelna = new SwitchOnAction(svKoupelna);
        Action offActionKoupelna = new SwitchOffAction(svKoupelna);
        lst.addActionBinding(new ActionBinding(koupelnaHoreSwA.getButton1(), new Action[]{offActionKoupelna}, null));
        lst.addActionBinding(new ActionBinding(koupelnaHoreSwA.getButton2(), new Action[]{onActionKoupelna}, null));
        lst.addActionBinding(new ActionBinding(koupelnaHoreSwA.getButton3(), new Action[]{onActionKoupelna}, null));
        lst.addActionBinding(new ActionBinding(koupelnaHoreSwA.getButton4(), new Action[]{offActionKoupelna}, null));

        Action onActionPradelna = new SwitchOnAction(svPradelna);
        Action offActionPradelna = new SwitchOffAction(svPradelna);
        lst.addActionBinding(new ActionBinding(chodbaDoldeSwA.getButton1(), new Action[]{offActionPradelna}, null));
        lst.addActionBinding(new ActionBinding(chodbaDoldeSwA.getButton2(), new Action[]{onActionPradelna}, null));
        lst.addActionBinding(new ActionBinding(chodbaDoldeSwA.getButton3(), new Action[]{onActionPradelna}, null));
        lst.addActionBinding(new ActionBinding(chodbaDoldeSwA.getButton4(), new Action[]{offActionPradelna}, null));

        Node pirNodeA = nodeInfoCollector.createNode(7, "PirNodeA");
        InputDevice pirA3Prizemi = new InputDevice("pirA3Prizemi", pirNodeA, 3);
        lst.addActionBinding(new ActionBinding(pirA3Prizemi.getIn5AndActivate(), new Action[]{new SwitchOffSensorAction(svSpajza, 10)}, new Action[]{new SwitchOnSensorAction(svSpajza, 600)}));

        InputDevice pirA1Prizemi = new InputDevice("pirA1Prizemi", pirNodeA, 1);
        lst.addActionBinding(new ActionBinding(pirA1Prizemi.getIn1AndActivate(), new Action[]{new SwitchOffSensorAction(svPradelna, 10)}, new Action[]{new SwitchOnSensorAction(svPradelna, 600)}));
        lst.addActionBinding(new ActionBinding(pirA1Prizemi.getIn2AndActivate(), new Action[]{new SwitchOffSensorAction(svPradelna, 10)}, new Action[]{new SwitchOnSensorAction(svPradelna, 600)}));

        // infrazaric v koupelne
        lst.addActionBinding(new ActionBinding(koupelnaHoreSwB.getButton1(), new Action[]{new SwitchOffAction(zaricKoupelnaHore2Trubice)}, null));
        lst.addActionBinding(new ActionBinding(koupelnaHoreSwB.getButton2(), new Action[]{new SwitchOnSensorAction(zaricKoupelnaHore2Trubice, 900)}, null));
        lst.addActionBinding(new ActionBinding(koupelnaHoreSwB.getButton3(), new Action[]{new SwitchOnSensorAction(zaricKoupelnaHore1Trubice, 900)}, null));
        lst.addActionBinding(new ActionBinding(koupelnaHoreSwB.getButton4(), new Action[]{new SwitchOffAction(zaricKoupelnaHore1Trubice)}, null));

        //WallSwitch chodbaDoleSwA = new WallSwitch("chodbaDoleSwA", chodbaDole, 1);
        //WallSwitch chodbaDoleSwB = new WallSwitch("chodbaDoleSwB", chodbaDole, 3);


//        actor3.initialize();
//        obyvakSpinacABC.initialize();


        RelayBoardDevice rele1ZaluzieAPort1 = new RelayBoardDevice("rele1ZaluzieAPort1", zaluzieA, 1);
        RelayBoardDevice rele1ZaluzieAPort2 = new RelayBoardDevice("rele1ZaluzieAPort1", zaluzieA, 2);
        RelayBoardDevice rele1ZaluzieAPort3 = new RelayBoardDevice("rele1ZaluzieAPort1", zaluzieA, 3);
        OnOffActor[] zaluzieActors = new OnOffActor[]{
                new OnOffActor("zaluzie01Up", rele1ZaluzieAPort1.getRele1(), 0, 1),
                new OnOffActor("zaluzie01Down", rele1ZaluzieAPort1.getRele2(), 0, 1),
                new OnOffActor("zaluzie02Up", rele1ZaluzieAPort1.getRele3(), 0, 1),
                new OnOffActor("zaluzie02Down", rele1ZaluzieAPort1.getRele4(), 0, 1),
                new OnOffActor("zaluzie03Up", rele1ZaluzieAPort1.getRele5(), 0, 1),
                new OnOffActor("zaluzie03Down", rele1ZaluzieAPort1.getRele6(), 0, 1),

                new OnOffActor("zaluzie04Up", rele1ZaluzieAPort2.getRele1(), 0, 1),
                new OnOffActor("zaluzie04Down", rele1ZaluzieAPort2.getRele2(), 0, 1),
                new OnOffActor("zaluzie05Up", rele1ZaluzieAPort2.getRele3(), 0, 1),
                new OnOffActor("zaluzie05Down", rele1ZaluzieAPort2.getRele4(), 0, 1),
                new OnOffActor("zaluzie06Up", rele1ZaluzieAPort2.getRele5(), 0, 1),
                new OnOffActor("zaluzie06Down", rele1ZaluzieAPort2.getRele6(), 0, 1),

                new OnOffActor("zaluzie07Up", rele1ZaluzieAPort3.getRele1(), 0, 1),
                new OnOffActor("zaluzie07Down", rele1ZaluzieAPort3.getRele2(), 0, 1),
                new OnOffActor("zaluzie08Up", rele1ZaluzieAPort3.getRele3(), 0, 1),
                new OnOffActor("zaluzie08Down", rele1ZaluzieAPort3.getRele4(), 0, 1),
                new OnOffActor("zaluzie09Up", rele1ZaluzieAPort3.getRele5(), 0, 1),
                new OnOffActor("zaluzie09Down", rele1ZaluzieAPort3.getRele6(), 0, 1),
        };

        Action[] zaluzieInvertActions = new Action[zaluzieActors.length];
        for (int i = 0; i < zaluzieActors.length; i++) {
            zaluzieInvertActions[i] = new InvertAction(zaluzieActors[i]);
            if (i % 2 == 1) {
                zaluzieActors[i].setConflictingActor(zaluzieActors[i - 1]);
                zaluzieActors[i - 1].setConflictingActor(zaluzieActors[i]);
            }
        }

        Servlet.action1 = onActionKoupelna;
        Servlet.action2 = offActionKoupelna;
        Servlet.action3 = invertJidelna;

        Servlet.action4 = null;
        Servlet.action5 = null;
        Servlet.zaluezieActions = zaluzieInvertActions;

        Node test10Node = nodeInfoCollector.createNode(10, "Test10");
        Node test12Node = nodeInfoCollector.createNode(12, "Test12");
        OutputDevice testOutputDevice3 = new OutputDevice("testOutputActor3", test10Node, 3, CpuFrequency.sixteenMHz);

        /*
        PwmActor testPwmActor = new PwmActor("testPWM", testOutputDevice3.getOut5(), 0, 1);
        Servlet.action4 = new IncreasePwmAction(testPwmActor);
        Servlet.action5 = new DecreasePwmAction(testPwmActor);
        */


//        OnOffActor testLedActor = new OnOffActor("testLed", testOutputDevice3.getOut2(), 1, 0);
//        lst.addActionBinding(new ActionBinding(testInputDevice2.getIn1(), new Action[]{new SensorAction(testLedActor, 10)}, new Action[]{new SensorAction(testLedActor, 60)}));


    }
}
