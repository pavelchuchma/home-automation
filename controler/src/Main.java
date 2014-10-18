import app.NodeInfoCollector;
import app.SwitchListener;
import controller.Action.Action;
import controller.Action.DecreasePwmAction;
import controller.Action.IncreasePwmAction;
import controller.Action.InvertAction;
import controller.Action.InvertActionWithTimer;
import controller.Action.SwitchOffAction;
import controller.Action.SwitchOffSensorAction;
import controller.Action.SwitchOnAction;
import controller.Action.SwitchOnSensorAction;
import controller.ActionBinding;
import controller.actor.OnOffActor;
import controller.actor.PwmActor;
import controller.actor.TestingOnOffActor;
import controller.device.InputDevice;
import controller.device.LddBoardDevice;
import controller.device.OutputDevice;
import controller.device.RelayBoardDevice;
import controller.device.SwitchIndicator;
import controller.device.WallSwitch;
import node.Node;
import node.NodePin;
import org.apache.log4j.Logger;
import packet.IPacketUartIO;
import packet.PacketUartIO;
import packet.PacketUartIOException;
import packet.PacketUartIOMock;
import servlet.Servlet;

import java.util.ArrayList;

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
        Node zaluzieB = nodeInfoCollector.createNode(4, "ZaluzieB");
        Node lozniceOkno = nodeInfoCollector.createNode(6, "LozniceOkno");
        Node chodbaDole = nodeInfoCollector.createNode(8, "ChodbaDole");
        Node koupelnaHore = nodeInfoCollector.createNode(9, "KoupelnaHore");
        Node vratnice = nodeInfoCollector.createNode(10, "Vratnice");
        Node obyvakSpinacABC = nodeInfoCollector.createNode(11, "ObyvakSpinacABC");
        Node krystof = nodeInfoCollector.createNode(12, "Krystof");
        Node zaluzieA = nodeInfoCollector.createNode(13, "ZaluzieA");

        WallSwitch obyvakA1Sw = new WallSwitch("obyvakA1Sw", obyvakSpinacABC, 1);
        WallSwitch obyvakA2Sw = new WallSwitch("obyvakA2Sw", obyvakSpinacABC, 2);
        WallSwitch obyvakA3Sw = new WallSwitch("obyvakA3Sw", obyvakSpinacABC, 3);
        OutputDevice triak1Actor3Port3 = new OutputDevice("triak1Actor3Port3", actor3, 3);
        RelayBoardDevice rele1Actor3Port2 = new RelayBoardDevice("rele1Actor3Port2", actor3, 2);

        WallSwitch koupelnaHoreSw1 = new WallSwitch("koupelnaHoreSw1", koupelnaHore, 1);
        WallSwitch koupelnaHoreSw2 = new WallSwitch("koupelnaHoreSw2", koupelnaHore, 2);
        WallSwitch chodbaDoldeSwA = new WallSwitch("chodbaDoldeSwA", chodbaDole, 1);
        WallSwitch lozniceOknoSwA = new WallSwitch("lozniceOknoSwA", lozniceOkno, 1);
        WallSwitch vratniceSw1 = new WallSwitch("vratniceSw1", vratnice, 1);
        WallSwitch vratniceSw2 = new WallSwitch("vratniceSw2", vratnice, 2);
        WallSwitch krystofSwA1 = new WallSwitch("krystofSwA1", krystof, 1);

        OnOffActor svKoupelna = new OnOffActor("svKoupelna", triak1Actor3Port3.getOut1(), 1, 0, obyvakA3Sw.getGreenLedIndicator(false), koupelnaHoreSw1.getGreenLedIndicator(false), koupelnaHoreSw1.getRedLedIndicator(true));
        OnOffActor svJidelna = new OnOffActor("svJidelna", triak1Actor3Port3.getOut2(), 1, 0, obyvakA3Sw.getRedLedIndicator(true));
        OnOffActor svSpajza = new OnOffActor("svSpajza", triak1Actor3Port3.getOut3(), 1, 0);
        OnOffActor svPradelna = new OnOffActor("svPradelna", triak1Actor3Port3.getOut4(), 1, 0, chodbaDoldeSwA.getGreenLedIndicator(false), chodbaDoldeSwA.getRedLedIndicator(true));
        OnOffActor zaricKoupelnaHore2Trubice = new OnOffActor("zaricKoupelnaHore2Trubice", rele1Actor3Port2.getRele1(), 0, 1, koupelnaHoreSw2.getRedLedIndicator(true));
        OnOffActor zaricKoupelnaHore1Trubice = new OnOffActor("zaricKoupelnaHore1Trubice", rele1Actor3Port2.getRele2(), 0, 1, koupelnaHoreSw2.getGreenLedIndicator(true));

        SwitchListener lst = nodeInfoCollector.getSwitchListener();

        Action invertJidelna = new InvertAction(svJidelna);
        lst.addActionBinding(new ActionBinding(obyvakA3Sw.getButton1(), new Action[]{new InvertAction(svKoupelna)}, null));
        lst.addActionBinding(new ActionBinding(obyvakA3Sw.getButton2(), new Action[]{invertJidelna}, null));

        Action onActionKoupelna = new SwitchOnAction(svKoupelna);
        Action offActionKoupelna = new SwitchOffAction(svKoupelna);
        lst.addActionBinding(new ActionBinding(koupelnaHoreSw1.getButton1(), new Action[]{offActionKoupelna}, null));
        lst.addActionBinding(new ActionBinding(koupelnaHoreSw1.getButton2(), new Action[]{onActionKoupelna}, null));

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
        lst.addActionBinding(new ActionBinding(koupelnaHoreSw2.getButton1(), new Action[]{new SwitchOffAction(zaricKoupelnaHore2Trubice)}, null));
        lst.addActionBinding(new ActionBinding(koupelnaHoreSw2.getButton2(), new Action[]{new SwitchOnSensorAction(zaricKoupelnaHore2Trubice, 900)}, null));
        lst.addActionBinding(new ActionBinding(koupelnaHoreSw2.getButton3(), new Action[]{new SwitchOnSensorAction(zaricKoupelnaHore1Trubice, 900)}, null));
        lst.addActionBinding(new ActionBinding(koupelnaHoreSw2.getButton4(), new Action[]{new SwitchOffAction(zaricKoupelnaHore1Trubice)}, null));


        // zaluzie
        RelayBoardDevice rele3ZaluzieAPort1 = new RelayBoardDevice("rele3ZaluzieAPort1", zaluzieA, 1);
        RelayBoardDevice rele4ZaluzieAPort2 = new RelayBoardDevice("rele3ZaluzieAPort1", zaluzieA, 2);
        RelayBoardDevice rele2ZaluzieAPort3 = new RelayBoardDevice("rele3ZaluzieAPort1", zaluzieA, 3);

        RelayBoardDevice rele6ZaluzieBPort1 = new RelayBoardDevice("rele6ZaluzieBPort1", zaluzieB, 1);
        RelayBoardDevice rele5ZaluzieBPort2 = new RelayBoardDevice("rele5ZaluzieBPort2", zaluzieB, 2);
        RelayBoardDevice rele7ZaluzieBPort3 = new RelayBoardDevice("rele7ZaluzieBPort3", zaluzieB, 3);

        RelayBoardDevice rele8Actor3Port1 = new RelayBoardDevice("rele8Actor3Port1", actor3, 1);

        OnOffActor zaluzieKrystofUp;
        OnOffActor zaluzieKrystofDown;
        OnOffActor zaluziePataUp;
        OnOffActor zaluziePataDown;
        OnOffActor zaluzieMarekUp;
        OnOffActor zaluzieMarekDown;

        OnOffActor zaluzieKoupelnaUp;
        OnOffActor zaluzieKoupelnaDown;
        OnOffActor zaluzieLoznice1Up;
        OnOffActor zaluzieLoznice1Down;
        OnOffActor zaluzieLoznice2Up;
        OnOffActor zaluzieLoznice2Down;

        OnOffActor zaluzieKuchynUp;
        OnOffActor zaluzieKuchynDown;
        OnOffActor zaluzieObyvak1Up;
        OnOffActor zaluzieObyvak1Down;
        OnOffActor zaluzieObyvak2Up;
        OnOffActor zaluzieObyvak2Down;
        OnOffActor zaluzieObyvak3Up;
        OnOffActor zaluzieObyvak3Down;
        OnOffActor zaluzieObyvak4Up;
        OnOffActor zaluzieObyvak4Down;
        OnOffActor zaluzieObyvak5Up;
        OnOffActor zaluzieObyvak5Down;
        OnOffActor zaluzieObyvak6Up;
        OnOffActor zaluzieObyvak6Down;
        OnOffActor zaluzieVratnice1Down;
        OnOffActor zaluzieVratnice1Up;
        OnOffActor zaluzieVratnice2Down;
        OnOffActor zaluzieVratnice2Up;
        OnOffActor zaluzieVratnice3Down;
        OnOffActor zaluzieVratnice3Up;

        OnOffActor[] louversActors = new OnOffActor[]{
                zaluzieKrystofUp = new OnOffActor("Kryštof Up", rele3ZaluzieAPort1.getRele1(), 0, 1),
                zaluzieKrystofDown = new OnOffActor("Kryštof Down", rele3ZaluzieAPort1.getRele2(), 0, 1),
                zaluziePataUp = new OnOffActor("Paťa Up", rele3ZaluzieAPort1.getRele3(), 0, 1),
                zaluziePataDown = new OnOffActor("Paťa Down", rele3ZaluzieAPort1.getRele4(), 0, 1),
                zaluzieMarekUp = new OnOffActor("Marek Up", rele4ZaluzieAPort2.getRele1(), 0, 1),
                zaluzieMarekDown = new OnOffActor("Marek Down", rele4ZaluzieAPort2.getRele2(), 0, 1),
                zaluzieKoupelnaUp = new OnOffActor("Koupelna Up", rele6ZaluzieBPort1.getRele1(), 0, 1),
                zaluzieKoupelnaDown = new OnOffActor("Koupelna Down", rele6ZaluzieBPort1.getRele2(), 0, 1),

                zaluzieLoznice1Up = new OnOffActor("Ložnice 1 Up", rele4ZaluzieAPort2.getRele5(), 0, 1, new SwitchIndicator(lozniceOknoSwA.getRedLed(), true)),
                zaluzieLoznice1Down = new OnOffActor("Ložnice 1 Down", rele4ZaluzieAPort2.getRele6(), 0, 1, new SwitchIndicator(lozniceOknoSwA.getRedLed(), true)),
                zaluzieLoznice2Up = new OnOffActor("Ložnice 2 Up", rele3ZaluzieAPort1.getRele5(), 0, 1, new SwitchIndicator(lozniceOknoSwA.getGreenLed(), true)),
                zaluzieLoznice2Down = new OnOffActor("Ložnice 2 Down", rele3ZaluzieAPort1.getRele6(), 0, 1, new SwitchIndicator(lozniceOknoSwA.getGreenLed(), true)),
                new OnOffActor("Šatna Up", rele8Actor3Port1.getRele3(), 0, 1),
                new OnOffActor("Šatna Down", rele8Actor3Port1.getRele4(), 0, 1),
                new OnOffActor("Pracovna Up", rele7ZaluzieBPort3.getRele1(), 0, 1),
                new OnOffActor("Pracovna Down", rele7ZaluzieBPort3.getRele2(), 0, 1),

                zaluzieKuchynUp = new OnOffActor("Kuchyně Up", rele2ZaluzieAPort3.getRele5(), 0, 1),
                zaluzieKuchynDown = new OnOffActor("Kuchyně Down", rele2ZaluzieAPort3.getRele6(), 0, 1),
                zaluzieObyvak1Up = new OnOffActor("Obývák 1 Up", rele2ZaluzieAPort3.getRele1(), 0, 1),
                zaluzieObyvak1Down = new OnOffActor("Obývák 1 Down", rele2ZaluzieAPort3.getRele2(), 0, 1),
                zaluzieObyvak2Up = new OnOffActor("Obývák 2 Up", rele8Actor3Port1.getRele5(), 0, 1),
                zaluzieObyvak2Down = new OnOffActor("Obývák 2 Down", rele8Actor3Port1.getRele6(), 0, 1),
                zaluzieObyvak3Up = new OnOffActor("Obývák 3 Up", rele2ZaluzieAPort3.getRele3(), 0, 1),
                zaluzieObyvak3Down = new OnOffActor("Obývák 3 Down", rele2ZaluzieAPort3.getRele4(), 0, 1),

                zaluzieObyvak4Up = new OnOffActor("Obývák 4 Up", rele4ZaluzieAPort2.getRele3(), 0, 1),
                zaluzieObyvak4Down = new OnOffActor("Obývák 4 Down", rele4ZaluzieAPort2.getRele4(), 0, 1),
                zaluzieObyvak5Up = new OnOffActor("Obývák 5 Up", rele7ZaluzieBPort3.getRele3(), 0, 1),
                zaluzieObyvak5Down = new OnOffActor("Obývák 5 Down", rele7ZaluzieBPort3.getRele4(), 0, 1),
                zaluzieObyvak6Up = new OnOffActor("Obývák 6 Up", rele7ZaluzieBPort3.getRele5(), 0, 1),
                zaluzieObyvak6Down = new OnOffActor("Obývák 6 Down", rele7ZaluzieBPort3.getRele6(), 0, 1),
                new OnOffActor("Chodba 1 Up", rele6ZaluzieBPort1.getRele3(), 0, 1),
                new OnOffActor("Chodba 1 Down", rele6ZaluzieBPort1.getRele4(), 0, 1),

                new OnOffActor("Chodba 2 Up", rele6ZaluzieBPort1.getRele5(), 0, 1),
                new OnOffActor("Chodba 2 Down", rele6ZaluzieBPort1.getRele6(), 0, 1),
                zaluzieVratnice1Up = new OnOffActor("Vrátnice 1 Up", rele5ZaluzieBPort2.getRele1(), 0, 1),
                zaluzieVratnice1Down = new OnOffActor("Vrátnice 1 Down", rele5ZaluzieBPort2.getRele2(), 0, 1),
                zaluzieVratnice2Up = new OnOffActor("Vrátnice 2 Up", rele5ZaluzieBPort2.getRele3(), 0, 1),
                zaluzieVratnice2Down = new OnOffActor("Vrátnice 2 Down", rele5ZaluzieBPort2.getRele4(), 0, 1),
                zaluzieVratnice3Up = new OnOffActor("Vrátnice 3 Up", rele5ZaluzieBPort2.getRele5(), 0, 1),
                zaluzieVratnice3Down = new OnOffActor("Vrátnice 3 Down", rele5ZaluzieBPort2.getRele6(), 0, 1),

                new OnOffActor("Koupelna 2 Up", rele8Actor3Port1.getRele1(), 0, 1),
                new OnOffActor("Koupelna 2 Down", rele8Actor3Port1.getRele2(), 0, 1),

        };

        Action[] louversInvertActions = new Action[louversActors.length];
        for (int i = 0; i < louversActors.length; i++) {
            louversInvertActions[i] = new InvertActionWithTimer(louversActors[i], 70);
            if (i % 2 == 1) {
                louversActors[i].setConflictingActor(louversActors[i - 1]);
                louversActors[i - 1].setConflictingActor(louversActors[i]);
            }
        }

        // koupelna
        configureLouvers(lst, true, koupelnaHoreSw1, zaluzieKoupelnaUp, zaluzieKoupelnaDown, 50);

        // kuchyn + obyvak
        configureLouvers(lst, true, obyvakA1Sw, zaluzieKuchynUp, zaluzieKuchynDown, 70);
        configureLouvers(lst, false, obyvakA1Sw, zaluzieObyvak1Up, zaluzieObyvak1Down, 70);
        configureLouvers(lst, true, obyvakA2Sw, zaluzieObyvak2Up, zaluzieObyvak2Down, zaluzieObyvak3Up, zaluzieObyvak3Down, 70);
        configureLouvers(lst, false, obyvakA2Sw, zaluzieObyvak4Up, zaluzieObyvak4Down, 70);
        configureLouvers(lst, true, obyvakA3Sw, zaluzieObyvak5Up, zaluzieObyvak5Down, zaluzieObyvak6Up, zaluzieObyvak6Down, 70);

        // koupelna
        configureLouvers(lst, true, koupelnaHoreSw1, zaluzieKoupelnaUp, zaluzieKoupelnaDown, 50);

        // Krystof + Pata
        configureLouvers(lst, true, krystofSwA1, zaluziePataUp, zaluziePataDown, 50);
        configureLouvers(lst, false, krystofSwA1, zaluzieKrystofUp, zaluzieKrystofDown, 50);

        // loznice
        configureLouvers(lst, true, lozniceOknoSwA, zaluzieLoznice1Up, zaluzieLoznice1Down, 40);
        configureLouvers(lst, false, lozniceOknoSwA, zaluzieLoznice2Up, zaluzieLoznice2Down, 40);

        // vratnice
        configureLouvers(lst, false, vratniceSw1, zaluzieVratnice3Up, zaluzieVratnice3Down, 50);
        configureLouvers(lst, true, vratniceSw2, zaluzieVratnice2Up, zaluzieVratnice2Down, 40);
        configureLouvers(lst, false, vratniceSw2, zaluzieVratnice1Up, zaluzieVratnice1Down, 40);

        Servlet.action1 = onActionKoupelna;
        Servlet.action2 = offActionKoupelna;
        Servlet.action3 = invertJidelna;

        Servlet.action4 = null;
        Servlet.action5 = null;
        Servlet.louversActions = louversInvertActions;

        Node testNode20 = nodeInfoCollector.createNode(20, "TestNode20");
        //test switch
        WallSwitch testSw = new WallSwitch("testSwA", testNode20, 2);
        TestingOnOffActor testingRightOnOffActor = new TestingOnOffActor("RightSwitchTestingActor", null, 0, 1, testSw.getRedLedIndicator(true));
        TestingOnOffActor testingLeftOnOffActor = new TestingOnOffActor("LeftSwitchTestingActor", null, 0, 1, testSw.getGreenLedIndicator(true));
        lst.addActionBinding(new ActionBinding(testSw.getButton1(), new Action[]{new SwitchOffAction(testingRightOnOffActor)}, null));
        lst.addActionBinding(new ActionBinding(testSw.getButton2(), new Action[]{new SwitchOnAction(testingRightOnOffActor)}, null));
        lst.addActionBinding(new ActionBinding(testSw.getButton3(), new Action[]{new SwitchOnAction(testingLeftOnOffActor)}, null));
        lst.addActionBinding(new ActionBinding(testSw.getButton4(), new Action[]{new SwitchOffAction(testingLeftOnOffActor)}, null));

        // lights
        // PWM
        LddBoardDevice testPwmDevice1 = new LddBoardDevice("testPwmDevice1", testNode20, 1);
        ArrayList<Action> lightsActions = new ArrayList<Action>();
        addLddLight(lightsActions, "Ldd1", testPwmDevice1.getLdd1());
        addLddLight(lightsActions, "Ldd2", testPwmDevice1.getLdd2());
        addLddLight(lightsActions, "Ldd3", testPwmDevice1.getLdd3());
        addLddLight(lightsActions, "Ldd4", testPwmDevice1.getLdd4());
        addLddLight(lightsActions, "Ldd5", testPwmDevice1.getLdd5());
        addLddLight(lightsActions, "Ldd6", testPwmDevice1.getLdd6());

        Servlet.lightsActions = lightsActions.toArray(new Action[lightsActions.size()]);

//        OnOffActor testLedActor = new OnOffActor("testLed", testOutputDevice3.getOut2(), 1, 0);
//        lst.addActionBinding(new ActionBinding(testInputDevice2.getIn1(), new Action[]{new SensorAction(testLedActor, 10)}, new Action[]{new SensorAction(testLedActor, 60)}));


    }

    static void addLddLight(ArrayList<Action> lightsActions, String name, NodePin pin) {
        PwmActor pwmA1 = new PwmActor(name,  pin, 0);
        lightsActions.add(new SwitchOnAction(pwmA1));
        lightsActions.add(new IncreasePwmAction(pwmA1));
        lightsActions.add(new DecreasePwmAction(pwmA1));
        lightsActions.add(new SwitchOffAction(pwmA1));
    }

    static void configureLouvers(SwitchListener lst, boolean left, WallSwitch wallSwitch, OnOffActor louversUp, OnOffActor louversDown, int duration) {
        NodePin upTrigger = (left) ? wallSwitch.getButton3() : wallSwitch.getButton2();
        NodePin downTrigger = (left) ? wallSwitch.getButton4() : wallSwitch.getButton1();

        lst.addActionBinding(new ActionBinding(upTrigger, new Action[]{new InvertActionWithTimer(louversUp, duration)}, null));
        lst.addActionBinding(new ActionBinding(downTrigger, new Action[]{new InvertActionWithTimer(louversDown, duration)}, null));
    }

    static void configureLouvers(SwitchListener lst, boolean left, WallSwitch wallSwitch, OnOffActor louvers1Up, OnOffActor louvers1Down, OnOffActor louvers2Up, OnOffActor louvers2Down, int duration) {
        NodePin upTrigger = (left) ? wallSwitch.getButton3() : wallSwitch.getButton2();
        NodePin downTrigger = (left) ? wallSwitch.getButton4() : wallSwitch.getButton1();

        lst.addActionBinding(new ActionBinding(upTrigger,
                new Action[]{
                        new InvertActionWithTimer(louvers1Up, duration),
                        new InvertActionWithTimer(louvers2Up, duration)
                }, null));
        lst.addActionBinding(new ActionBinding(downTrigger,
                new Action[]{
                        new InvertActionWithTimer(louvers1Down, duration),
                        new InvertActionWithTimer(louvers2Down, duration)
                }, null));
    }

}
