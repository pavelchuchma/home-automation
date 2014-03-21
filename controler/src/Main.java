import app.NodeInfoCollector;
import app.SwitchListener;
import controller.Action.*;
import controller.ActionBinding;
import controller.actor.OnOffActor;
import controller.actor.PwmActor;
import controller.device.InputDevice;
import controller.device.OutputDevice;
import controller.device.WallSwitch;
import node.CpuFrequency;
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

        Node brige = nodeInfoCollector.createNode(1, "Bridge");
        Node triak1 = nodeInfoCollector.createNode(3, "Triak1");
        Node obyvakSpinacABC = nodeInfoCollector.createNode(11, "ObyvakSpinacABC");
        Node chodbaDole = nodeInfoCollector.createNode(8, "ChodbaDole");
        Node koupelnaHore = nodeInfoCollector.createNode(10, "KoupelnaHore");

        WallSwitch obyvakA3sw = new WallSwitch("obyvakA3sw", obyvakSpinacABC, 3);
        OutputDevice triak1OutputPort2 = new OutputDevice("triak1OutputPort2", triak1, 2);

        WallSwitch koupelnaHoreSwA = new WallSwitch("koupelnaHoreSwA", koupelnaHore, 1);
        WallSwitch chodbaDoldeSwA = new WallSwitch("chodbaDoldeSwA", chodbaDole, 1);

        OnOffActor svKoupelna = new OnOffActor("svKoupelna", triak1OutputPort2.getOut1(), 1, 0, obyvakA3sw.getGreenLedIndicator(false), koupelnaHoreSwA.getGreenLedIndicator(false), koupelnaHoreSwA.getRedLedIndicator(true));
        OnOffActor svJidelna = new OnOffActor("svJidelna", triak1OutputPort2.getOut2(), 1, 0, obyvakA3sw.getRedLedIndicator(true));
        OnOffActor svSpajza = new OnOffActor("svSpajza", triak1OutputPort2.getOut3(), 1, 0);
        OnOffActor svPradelna = new OnOffActor("svPradelna", triak1OutputPort2.getOut4(), 1, 0, chodbaDoldeSwA.getGreenLedIndicator(false), chodbaDoldeSwA.getRedLedIndicator(true));

        SwitchListener lst = nodeInfoCollector.getSwitchListener();

        Action invertJidelna = new InvertAction(svJidelna);
        lst.addActionBinding(new ActionBinding(obyvakA3sw.getButton1(), new Action[]{new InvertAction(svKoupelna)}, null));
        lst.addActionBinding(new ActionBinding(obyvakA3sw.getButton2(), new Action[]{invertJidelna}, null));
        lst.addActionBinding(new ActionBinding(obyvakA3sw.getButton3(), new Action[]{new InvertAction(svSpajza)}, null));
        lst.addActionBinding(new ActionBinding(obyvakA3sw.getButton4(), new Action[]{new InvertAction(svPradelna)}, null));

        Action onActionKoupelna = new SwitchOnAction(svKoupelna);
        Action offActionKoupelna = new SwitchOffAction(svKoupelna);
        lst.addActionBinding(new ActionBinding(koupelnaHoreSwA.getButton1(), new Action[]{offActionKoupelna}, null));
        lst.addActionBinding(new ActionBinding(koupelnaHoreSwA.getButton2(), new Action[]{onActionKoupelna}, null));
        lst.addActionBinding(new ActionBinding(koupelnaHoreSwA.getButton3(), new Action[]{onActionKoupelna}, null));
        lst.addActionBinding(new ActionBinding(koupelnaHoreSwA.getButton4(), new Action[]{offActionKoupelna}, null));

        Action onActionPradelna = new SwitchOnAction(svPradelna);
        Action offActionPradelna = new SwitchOffAction(svPradelna);
        Action sensorPradelna = new SwitchOnSensorAction(svPradelna, 10);
        lst.addActionBinding(new ActionBinding(chodbaDoldeSwA.getButton1(), new Action[]{offActionPradelna}, null));
        lst.addActionBinding(new ActionBinding(chodbaDoldeSwA.getButton2(), new Action[]{onActionPradelna}, null));
        lst.addActionBinding(new ActionBinding(chodbaDoldeSwA.getButton3(), new Action[]{onActionPradelna}, null));
        lst.addActionBinding(new ActionBinding(chodbaDoldeSwA.getButton4(), new Action[]{sensorPradelna}, null));



        Node pirNodeA = nodeInfoCollector.createNode(7, "PirNodeA");
        InputDevice pirA1Prizemi = new InputDevice("PirA1Prizemi", pirNodeA, 3);
        lst.addActionBinding(new ActionBinding(pirA1Prizemi.getIn5AndActivate(), new Action[]{new SwitchOffSensorAction(svSpajza, 10)}, new Action[]{new SwitchOnSensorAction(svSpajza, 600)}));


        //WallSwitch chodbaDoleSwA = new WallSwitch("chodbaDoleSwA", chodbaDole, 1);
        //WallSwitch chodbaDoleSwB = new WallSwitch("chodbaDoleSwB", chodbaDole, 3);


//        triak1.initialize();
//        obyvakSpinacABC.initialize();

        Servlet.action1 = onActionKoupelna;
        Servlet.action2 = offActionKoupelna;
        Servlet.action3 = invertJidelna;

        Node testNode = nodeInfoCollector.createNode(9, "Test");
        OutputDevice testOutputDevice3 = new OutputDevice("testOutputActor3", testNode, 3, CpuFrequency.sixteenMHz);

        PwmActor testPwmActor = new PwmActor("testPWM", testOutputDevice3.getOut5(), 0, 1);
        Servlet.action4 = new IncreasePwmAction(testPwmActor);
        Servlet.action5 = new DecreasePwmAction(testPwmActor);


//        OnOffActor testLedActor = new OnOffActor("testLed", testOutputDevice3.getOut2(), 1, 0);
//        lst.addActionBinding(new ActionBinding(testInputDevice2.getIn1(), new Action[]{new SensorAction(testLedActor, 10)}, new Action[]{new SensorAction(testLedActor, 60)}));



    }
}
