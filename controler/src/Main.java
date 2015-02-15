import app.NodeInfoCollector;
import app.SwitchListener;
import controller.Action.Action;
import controller.Action.DecreasePwmAction;
import controller.Action.IncreasePwmAction;
import controller.Action.InvertAction;
import controller.Action.InvertActionWithTimer;
import controller.Action.PwmActionGroup;
import controller.Action.SwitchOffAction;
import controller.Action.SwitchOffSensorAction;
import controller.Action.SwitchOnAction;
import controller.Action.SwitchOnSensorAction;
import controller.ActionBinding;
import controller.PirStatus;
import controller.actor.Indicator;
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
import java.util.List;

public class Main {
    static Logger log = Logger.getLogger(Main.class.getName());
    static List<PirStatus> pirStatusList = new ArrayList<PirStatus>();


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
            e.printStackTrace();
            System.exit(2);
        } catch (Exception e) {
            log.error("Initialization failed", e);
            e.printStackTrace();
            System.exit(3);
        }
    }

    private static void configure(NodeInfoCollector nodeInfoCollector) {

        Node bridge = nodeInfoCollector.createNode(1, "Bridge");
        Node zadveri = nodeInfoCollector.createNode(2, "Zadveri");
        Node actor3 = nodeInfoCollector.createNode(3, "Actor3");
        Node zaluzieB = nodeInfoCollector.createNode(4, "ZaluzieB");
        Node lozniceOkno = nodeInfoCollector.createNode(6, "LozniceOkno");
        Node pirNodeA = nodeInfoCollector.createNode(7, "PirNodeA");
        Node zadveriDoleChodba = nodeInfoCollector.createNode(8, "ZadveriDoleUChodby");
        Node koupelnaHore = nodeInfoCollector.createNode(9, "KoupelnaHore");
        Node vratnice = nodeInfoCollector.createNode(10, "Vratnice");
        Node obyvakSpinacABC = nodeInfoCollector.createNode(11, "ObyvakSpinacABC");
        Node krystof = nodeInfoCollector.createNode(12, "Krystof");
        Node zaluzieA = nodeInfoCollector.createNode(13, "ZaluzieA");
        Node marek = nodeInfoCollector.createNode(14, "Marek");
        Node patrik = nodeInfoCollector.createNode(15, "Patrik");
        Node chodbaA = nodeInfoCollector.createNode(16, "ChodbaA");
        Node zadveriDoleVchod = nodeInfoCollector.createNode(17, "ZadveriDoleUVchodu");
        Node pracovna = nodeInfoCollector.createNode(18, "Pracovna");
        Node lddActorA = nodeInfoCollector.createNode(19, "LDD-ActorA");
        Node testNode20 = nodeInfoCollector.createNode(20, "TestNode20");
        Node lozniceDvere = nodeInfoCollector.createNode(21, "LozniceDvere");
        Node pradelna = nodeInfoCollector.createNode(23, "Pradelna");
        Node koupelnaDole = nodeInfoCollector.createNode(25, "KoupelnaDole");

        WallSwitch chodbaA1Sw = new WallSwitch("chodbaA1Sw", chodbaA, 1);
        WallSwitch chodbaA2Sw = new WallSwitch("chodbaA2Sw", chodbaA, 2);
        WallSwitch wcSw = new WallSwitch("wcSw", chodbaA, 3);

        WallSwitch obyvakA1Sw = new WallSwitch("obyvakA1Sw", obyvakSpinacABC, 1);
        WallSwitch obyvakA2Sw = new WallSwitch("obyvakA2Sw", obyvakSpinacABC, 2);
        WallSwitch obyvakA3Sw = new WallSwitch("obyvakA3Sw", obyvakSpinacABC, 3);
        OutputDevice triak1Actor3Port3 = new OutputDevice("triak1Actor3Port3", actor3, 3);
        RelayBoardDevice rele1Actor3Port2 = new RelayBoardDevice("rele1Actor3Port2", actor3, 2);

        WallSwitch zadveriSwA1 = new WallSwitch("zadveriSwA1", zadveri, 1);

        WallSwitch koupelnaHoreSw1 = new WallSwitch("koupelnaHoreSw1", koupelnaHore, 1);
        WallSwitch koupelnaHoreSw2 = new WallSwitch("koupelnaHoreSw2", koupelnaHore, 2);
        WallSwitch chodbaHoreKoupelnaSw3 = new WallSwitch("chodbaHoreKoupelnaSw3", koupelnaHore, 3);
        WallSwitch zadveriDoleChodbaSw = new WallSwitch("zadveriDoleChodbaSw", zadveriDoleChodba, 1);
        WallSwitch zadveriDoldePradelnaSw = new WallSwitch("zadveriDoldePradelnaSw", zadveriDoleChodba, 3);
        WallSwitch lozniceOknoSw1 = new WallSwitch("lozniceOknoSw1", lozniceOkno, 1);
        WallSwitch lozniceOknoSw2 = new WallSwitch("lozniceOknoSw2", lozniceOkno, 2);
        WallSwitch lozniceDvereSw1 = new WallSwitch("lozniceDvereSw1", lozniceDvere, 1);
        WallSwitch lozniceDvereSw2 = new WallSwitch("lozniceDvereSw2", lozniceDvere, 2);
        WallSwitch marekPostelSw3 = new WallSwitch("marekPostelSw3", lozniceDvere, 3);
        WallSwitch vratniceSw1 = new WallSwitch("vratniceSw1", vratnice, 1);
        WallSwitch vratniceSw2 = new WallSwitch("vratniceSw2", vratnice, 2);
        WallSwitch zadveriVratniceSw3 = new WallSwitch("zadveriVratniceSw3", vratnice, 3);
        WallSwitch krystofSwA1 = new WallSwitch("krystofSwA1", krystof, 1);
        WallSwitch krystofSwA2 = new WallSwitch("krystofSwA2", krystof, 2);
        WallSwitch marekSwA1 = new WallSwitch("marekSwA1", marek, 1);
        WallSwitch krystofPostelSw = new WallSwitch("krystofPostel", zadveriDoleChodba, 2);
        WallSwitch patrikSw1 = new WallSwitch("pataSw1", patrik, 1);
        WallSwitch pracovnaSw2 = new WallSwitch("pracovnaSw2", pracovna, 2);
        WallSwitch satnaSw3 = new WallSwitch("satnaSw3", pracovna, 3);
        WallSwitch koupelnaHoreOknoSw = new WallSwitch("koupelnaHoreOknoSw", zadveriDoleVchod, 3);
        WallSwitch pradelnaSw1 = new WallSwitch("pradelnaSw1", pradelna, 1);
        WallSwitch chodbaDoleSpajzSw3 = new WallSwitch("chodbaDoleSpajzSw3", pradelna, 3);

//        OnOffActor svKoupelna = new OnOffActor("svKoupelna", triak1Actor3Port3.getOut1(), 1, 0, obyvakA3Sw.getGreenLedIndicator(false), koupelnaHoreSw1.getGreenLedIndicator(false), koupelnaHoreSw1.getRedLedIndicator(true));
        OnOffActor svJidelna = new OnOffActor("svJidelna", triak1Actor3Port3.getOut1(), 1, 0, obyvakA3Sw.getRedLedIndicator(true));
        OnOffActor svSklepLevy = new OnOffActor("svSklepLevy", triak1Actor3Port3.getOut2(), 1, 0, zadveriDoldePradelnaSw.getGreenLedIndicator(false));
        OnOffActor svSpajza = new OnOffActor("svSpajza", triak1Actor3Port3.getOut3(), 1, 0, chodbaDoleSpajzSw3.getRedLedIndicator(false));
        OnOffActor svPradelna = new OnOffActor("svPradelna", triak1Actor3Port3.getOut4(), 1, 0, zadveriDoldePradelnaSw.getRedLedIndicator(false), pradelnaSw1.getRedLedIndicator(true));
        OnOffActor zaricKoupelnaHore2Trubice = new OnOffActor("zaricKoupelnaHore2Trubice", rele1Actor3Port2.getRele1(), 0, 1, koupelnaHoreSw2.getRedLedIndicator(true), koupelnaHoreOknoSw.getRedLedIndicator(true));
        OnOffActor zaricKoupelnaHore1Trubice = new OnOffActor("zaricKoupelnaHore1Trubice", rele1Actor3Port2.getRele2(), 0, 1, koupelnaHoreSw2.getGreenLedIndicator(true), koupelnaHoreOknoSw.getGreenLedIndicator(true));

        SwitchListener lst = nodeInfoCollector.getSwitchListener();

        Action invertJidelna = new InvertAction(svJidelna);
//        lst.addActionBinding(new ActionBinding(obyvakA3Sw.getRightBottomButton(), new Action[]{new InvertAction(svKoupelna)}, null));
        lst.addActionBinding(new ActionBinding(obyvakA3Sw.getRightUpperButton(), new Action[]{invertJidelna}, null));

//        Action onActionKoupelna = new SwitchOnAction(svKoupelna);
//        Action offActionKoupelna = new SwitchOffAction(svKoupelna);
//        lst.addActionBinding(new ActionBinding(koupelnaHoreSw1.getRightBottomButton(), new Action[]{offActionKoupelna}, null));
//        lst.addActionBinding(new ActionBinding(koupelnaHoreSw1.getRightUpperButton(), new Action[]{onActionKoupelna}, null));


        // infrazaric v koupelne
        lst.addActionBinding(new ActionBinding(koupelnaHoreSw2.getRightBottomButton(), new Action[]{new SwitchOffAction(zaricKoupelnaHore2Trubice)}, null));
        lst.addActionBinding(new ActionBinding(koupelnaHoreSw2.getRightUpperButton(), new Action[]{new SwitchOnSensorAction(zaricKoupelnaHore2Trubice, 900)}, null));
        lst.addActionBinding(new ActionBinding(koupelnaHoreSw2.getLeftUpperButton(), new Action[]{new SwitchOnSensorAction(zaricKoupelnaHore1Trubice, 900)}, null));
        lst.addActionBinding(new ActionBinding(koupelnaHoreSw2.getLeftBottomButton(), new Action[]{new SwitchOffAction(zaricKoupelnaHore1Trubice)}, null));


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
        OnOffActor zaluzieSatnaUp;
        OnOffActor zaluzieSatnaDown;
        OnOffActor zaluziePracovnaUp;
        OnOffActor zaluziePracovnaDown;
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
        OnOffActor zaluzieChodba1Up;
        OnOffActor zaluzieChodba1Down;
        OnOffActor zaluzieChodba2Up;
        OnOffActor zaluzieChodba2Down;
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

                zaluzieLoznice1Up = new OnOffActor("Ložnice 1 Up", rele4ZaluzieAPort2.getRele5(), 0, 1, new SwitchIndicator(lozniceOknoSw1.getRedLed(), true)),
                zaluzieLoznice1Down = new OnOffActor("Ložnice 1 Down", rele4ZaluzieAPort2.getRele6(), 0, 1, new SwitchIndicator(lozniceOknoSw1.getRedLed(), true)),
                zaluzieLoznice2Up = new OnOffActor("Ložnice 2 Up", rele3ZaluzieAPort1.getRele5(), 0, 1, new SwitchIndicator(lozniceOknoSw1.getGreenLed(), true)),
                zaluzieLoznice2Down = new OnOffActor("Ložnice 2 Down", rele3ZaluzieAPort1.getRele6(), 0, 1, new SwitchIndicator(lozniceOknoSw1.getGreenLed(), true)),
                zaluzieSatnaUp = new OnOffActor("Šatna Up", rele8Actor3Port1.getRele3(), 0, 1),
                zaluzieSatnaDown = new OnOffActor("Šatna Down", rele8Actor3Port1.getRele4(), 0, 1),
                zaluziePracovnaUp = new OnOffActor("Pracovna Up", rele7ZaluzieBPort3.getRele1(), 0, 1),
                zaluziePracovnaDown = new OnOffActor("Pracovna Down", rele7ZaluzieBPort3.getRele2(), 0, 1),

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
                zaluzieChodba1Up = new OnOffActor("Chodba 1 Up", rele6ZaluzieBPort1.getRele3(), 0, 1),
                zaluzieChodba1Down = new OnOffActor("Chodba 1 Down", rele6ZaluzieBPort1.getRele4(), 0, 1),

                zaluzieChodba2Up = new OnOffActor("Chodba 2 Up", rele6ZaluzieBPort1.getRele5(), 0, 1),
                zaluzieChodba2Down = new OnOffActor("Chodba 2 Down", rele6ZaluzieBPort1.getRele6(), 0, 1),
                zaluzieVratnice1Up = new OnOffActor("Vrátnice 1 Up", rele5ZaluzieBPort2.getRele1(), 0, 1),
                zaluzieVratnice1Down = new OnOffActor("Vrátnice 1 Down", rele5ZaluzieBPort2.getRele2(), 0, 1),
                zaluzieVratnice2Up = new OnOffActor("Vrátnice 2 Up", rele5ZaluzieBPort2.getRele3(), 0, 1),
                zaluzieVratnice2Down = new OnOffActor("Vrátnice 2 Down", rele5ZaluzieBPort2.getRele4(), 0, 1),
                zaluzieVratnice3Up = new OnOffActor("Vrátnice 3 Up", rele5ZaluzieBPort2.getRele5(), 0, 1),
                zaluzieVratnice3Down = new OnOffActor("Vrátnice 3 Down", rele5ZaluzieBPort2.getRele6(), 0, 1),

                new OnOffActor("Koupelna 2 Up", rele8Actor3Port1.getRele1(), 0, 1),
                new OnOffActor("Koupelna 2 Down", rele8Actor3Port1.getRele2(), 0, 1),

        };

        // lights
        // PWM
        LddBoardDevice lddDevice1 = new LddBoardDevice("lddDevice1", lddActorA, 1, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0);
        ArrayList<Action> lightsActions = new ArrayList<Action>();
        PwmActor marekPwmActor = addLddLight(lightsActions, "Marek", lddDevice1.getLdd1(), 0.95, new SwitchIndicator(marekSwA1.getRedLed(), true), new SwitchIndicator(marekPostelSw3.getRedLed(), true)); //.96
        PwmActor pataPwmActor = addLddLight(lightsActions, "Paťa", lddDevice1.getLdd2(), 0.95, new SwitchIndicator(patrikSw1.getRedLed(), true)); //.96
        PwmActor krystofPwmActor = addLddLight(lightsActions, "Kryštof", lddDevice1.getLdd3(), 0.95, new SwitchIndicator(krystofSwA2.getRedLed(), true), new SwitchIndicator(krystofPostelSw.getRedLed(), true)); //.96
        PwmActor koupelnaPwmActor = addLddLight(lightsActions, "Koupelna", lddDevice1.getLdd4(), 1.0, obyvakA3Sw.getGreenLedIndicator(false), koupelnaHoreSw1.getGreenLedIndicator(false), koupelnaHoreSw1.getRedLedIndicator(true)); // 1.08
        PwmActor loznice1PwmActor = addLddLight(lightsActions, "Ložnice velké", lddDevice1.getLdd5(), 1.0); //1.08
        PwmActor satnaPwmActor = addLddLight(lightsActions, "Šatna", lddDevice1.getLdd6(), 0.30, new SwitchIndicator(chodbaA1Sw.getRedLed(), true), new SwitchIndicator(satnaSw3.getRedLed(), true)); //0.48

        LddBoardDevice lddDevice2 = new LddBoardDevice("lddDevice2", lddActorA, 2, 1.0, .7, .7, .7, .7, .35);
        PwmActor vratnice1PwmActor = addLddLight(lightsActions, "Vrátnice 1", lddDevice2.getLdd1(), 0.95, new SwitchIndicator(vratniceSw1.getRedLed(), true)); // .96
        PwmActor vratnice2PwmActor = addLddLight(lightsActions, "Vrátnice 2", lddDevice2.getLdd2(), 0.48); //.48
        PwmActor pracovnaPwmActor = addLddLight(lightsActions, "Pracovna", lddDevice2.getLdd3(), 0.5); // .6
        PwmActor chodbaDolePwmActor = addLddLight(lightsActions, "Chodba dole", lddDevice2.getLdd4(), 0.45); //.48
        PwmActor zadveriDolePwmActor = addLddLight(lightsActions, "Zádveří dole", lddDevice2.getLdd5(), 0.45, zadveriDoleChodbaSw.getGreenLedIndicator(false), zadveriDoleChodbaSw.getRedLedIndicator(true)); // .48
        PwmActor wcPwmActor = addLddLight(lightsActions, "WC", lddDevice2.getLdd6(), 0.24);

        LddBoardDevice lddDevice3 = new LddBoardDevice("lddDevice3", lddActorA, 3, .35, .35, .35, .35, .7, .7);
        PwmActor loznice2PwmActor = addLddLight(lightsActions, "Ložnice malé", lddDevice3.getLdd1(), 0.35, new SwitchIndicator(lozniceDvereSw2.getRedLed(), true), new SwitchIndicator(lozniceOknoSw2.getRedLed(), true)); // .36
        PwmActor koupelnaZrcadlaPwmActor = addLddLight(lightsActions, "Koupena zrcadla", lddDevice3.getLdd2(), 0.35); // .36
        PwmActor zadveriPwmActor = addLddLight(lightsActions, "Zádveří", lddDevice3.getLdd3(), 0.35, new SwitchIndicator(zadveriSwA1.getRedLed(), true)); //0.48
        PwmActor test34PwmActor = addLddLight(lightsActions, "test34", lddDevice3.getLdd4(), 0.01); // TODO
        PwmActor test35PwmActor = addLddLight(lightsActions, "test35", lddDevice3.getLdd5(), 0.01); // TODO
        PwmActor test36PwmActor = addLddLight(lightsActions, "test36", lddDevice3.getLdd6(), 0.01); // TODO

        Action[] louversInvertActions = new Action[louversActors.length];
        for (int i = 0; i < louversActors.length; i++) {
            louversInvertActions[i] = new InvertActionWithTimer(louversActors[i], 70);
            if (i % 2 == 1) {
                louversActors[i].setConflictingActor(louversActors[i - 1]);
                louversActors[i - 1].setConflictingActor(louversActors[i]);
            }
        }

        // koupelna
        configureLouvers(lst, koupelnaHoreSw1, WallSwitch.Side.LEFT, zaluzieKoupelnaUp, zaluzieKoupelnaDown, 50);
        configurePwmLights(lst, koupelnaHoreSw1, WallSwitch.Side.RIGHT, 25, koupelnaPwmActor);
        lst.addActionBinding(new ActionBinding(obyvakA3Sw.getRightBottomButton(), new Action[]{new InvertAction(koupelnaPwmActor, 30)}, null));
        //configurePwmLights(lst, obyvakA3Sw, WallSwitch.Side.RIGHT, 50, koupelnaPwmActor);

        // koupelna u okna
        configureLouvers(lst, koupelnaHoreOknoSw, WallSwitch.Side.LEFT, zaluzieKoupelnaUp, zaluzieKoupelnaDown, 50);
        lst.addActionBinding(new ActionBinding(koupelnaHoreOknoSw.getRightUpperButton(), new Action[]{new SwitchOnSensorAction(zaricKoupelnaHore1Trubice, 900), new SwitchOnSensorAction(zaricKoupelnaHore2Trubice, 900)}, null));
        lst.addActionBinding(new ActionBinding(koupelnaHoreOknoSw.getRightBottomButton(), new Action[]{new SwitchOffAction(zaricKoupelnaHore1Trubice), new SwitchOffAction(zaricKoupelnaHore2Trubice)}, null));


        // kuchyn + obyvak
        configureLouvers(lst, obyvakA1Sw, WallSwitch.Side.LEFT, zaluzieKuchynUp, zaluzieKuchynDown, 70);
        configureLouvers(lst, obyvakA1Sw, WallSwitch.Side.RIGHT, zaluzieObyvak1Up, zaluzieObyvak1Down, 70);
        configureLouvers(lst, WallSwitch.Side.LEFT, obyvakA2Sw, zaluzieObyvak2Up, zaluzieObyvak2Down, zaluzieObyvak3Up, zaluzieObyvak3Down, 70);
        configureLouvers(lst, obyvakA2Sw, WallSwitch.Side.RIGHT, zaluzieObyvak4Up, zaluzieObyvak4Down, 70);
        configureLouvers(lst, WallSwitch.Side.LEFT, obyvakA3Sw, zaluzieObyvak5Up, zaluzieObyvak5Down, zaluzieObyvak6Up, zaluzieObyvak6Down, 70);

        // wc
        configurePwmLights(lst, wcSw, WallSwitch.Side.LEFT, 60, wcPwmActor);
        configurePwmLights(lst, wcSw, WallSwitch.Side.RIGHT, 60, wcPwmActor);


        // chodba
        // svetla satna
        configurePwmLights(lst, chodbaA1Sw, WallSwitch.Side.LEFT, 80, satnaPwmActor);
        configureLouvers(lst, chodbaA1Sw, WallSwitch.Side.RIGHT, zaluzieSatnaUp, zaluzieSatnaDown, 50);

        configurePwmLights(lst, satnaSw3, WallSwitch.Side.RIGHT, 80, satnaPwmActor);
        configureLouvers(lst, satnaSw3, WallSwitch.Side.LEFT, zaluzieSatnaUp, zaluzieSatnaDown, 50);

        // zadveri
        configurePwmLights(lst, zadveriSwA1, WallSwitch.Side.LEFT, 80, zadveriPwmActor);
        configurePwmLights(lst, zadveriSwA1, WallSwitch.Side.RIGHT, 80, zadveriPwmActor);
        configurePwmLights(lst, zadveriVratniceSw3, WallSwitch.Side.LEFT, 80, zadveriPwmActor);
        configurePwmLights(lst, zadveriVratniceSw3, WallSwitch.Side.RIGHT, 80, zadveriPwmActor);
        configurePwmLights(lst, chodbaHoreKoupelnaSw3, WallSwitch.Side.LEFT, 80, zadveriPwmActor);

        // switch off 4 lights
        lst.addActionBinding(new ActionBinding(chodbaHoreKoupelnaSw3.getRightBottomButton(),
                new Action[]{new SwitchOffAction(pataPwmActor), new SwitchOffAction(krystofPwmActor),
                        new SwitchOffAction(marekPwmActor), new SwitchOffAction(satnaPwmActor)}, null));

        // Krystof + Pata
        configureLouvers(lst, krystofSwA1, WallSwitch.Side.LEFT, zaluziePataUp, zaluziePataDown, 50);
        configureLouvers(lst, krystofSwA1, WallSwitch.Side.RIGHT, zaluzieKrystofUp, zaluzieKrystofDown, 50);
        configurePwmLights(lst, krystofSwA2, WallSwitch.Side.LEFT, 50, pataPwmActor);
        configurePwmLights(lst, krystofSwA2, WallSwitch.Side.RIGHT, 50, krystofPwmActor);

        configurePwmLights(lst, patrikSw1, WallSwitch.Side.LEFT, 50, pataPwmActor);
        configurePwmLights(lst, patrikSw1, WallSwitch.Side.RIGHT, 50, krystofPwmActor);

        configurePwmLights(lst, krystofPostelSw, WallSwitch.Side.LEFT, 50, krystofPwmActor);
        configureLouvers(lst, krystofPostelSw, WallSwitch.Side.RIGHT, zaluzieKrystofUp, zaluzieKrystofDown, 50);

        // Marek
        configureLouvers(lst, marekSwA1, WallSwitch.Side.LEFT, zaluzieMarekUp, zaluzieMarekDown, 50);
        configurePwmLights(lst, marekSwA1, WallSwitch.Side.RIGHT, 50, marekPwmActor);
        configureLouvers(lst, marekPostelSw3, WallSwitch.Side.LEFT, zaluzieMarekUp, zaluzieMarekDown, 50);
        configurePwmLights(lst, marekPostelSw3, WallSwitch.Side.RIGHT, 50, marekPwmActor);


        // chodba
        configureLouvers(lst, chodbaA2Sw, WallSwitch.Side.LEFT, zaluzieChodba1Up, zaluzieChodba1Down, 50);
        configureLouvers(lst, chodbaA2Sw, WallSwitch.Side.RIGHT, zaluzieChodba2Up, zaluzieChodba2Down, 50);

        // loznice
        configureLouvers(lst, lozniceOknoSw1, WallSwitch.Side.LEFT, zaluzieLoznice1Up, zaluzieLoznice1Down, 40);
        configureLouvers(lst, lozniceOknoSw1, WallSwitch.Side.RIGHT, zaluzieLoznice2Up, zaluzieLoznice2Down, 40);
        configureLouvers(lst, lozniceDvereSw1, WallSwitch.Side.LEFT, zaluzieLoznice1Up, zaluzieLoznice1Down, 40);
        configureLouvers(lst, lozniceDvereSw1, WallSwitch.Side.RIGHT, zaluzieLoznice2Up, zaluzieLoznice2Down, 40);
        configurePwmLights(lst, lozniceDvereSw2, WallSwitch.Side.LEFT, 40, loznice2PwmActor);
        configurePwmLights(lst, lozniceDvereSw2, WallSwitch.Side.RIGHT, 40, loznice1PwmActor);
        configurePwmLights(lst, lozniceOknoSw2, WallSwitch.Side.LEFT, 40, loznice1PwmActor);
        configurePwmLights(lst, lozniceOknoSw2, WallSwitch.Side.RIGHT, 40, loznice2PwmActor);

        //pracovna
        configureLouvers(lst, pracovnaSw2, WallSwitch.Side.LEFT, zaluziePracovnaUp, zaluziePracovnaDown, 70);
        configurePwmLights(lst, pracovnaSw2, WallSwitch.Side.RIGHT, 30, pracovnaPwmActor);

        // vratnice
        configureLouvers(lst, vratniceSw1, WallSwitch.Side.RIGHT, zaluzieVratnice1Up, zaluzieVratnice1Down, 50);
        configureLouvers(lst, vratniceSw2, WallSwitch.Side.LEFT, zaluzieVratnice2Up, zaluzieVratnice2Down, 40);
        configureLouvers(lst, vratniceSw2, WallSwitch.Side.RIGHT, zaluzieVratnice3Up, zaluzieVratnice3Down, 40);
        configurePwmLights(lst, vratniceSw1, WallSwitch.Side.LEFT, 40, vratnice1PwmActor, vratnice2PwmActor);

        // zadveri dole
        lst.addActionBinding(new ActionBinding(zadveriDoldePradelnaSw.getLeftUpperButton(), new Action[]{new SwitchOnSensorAction(svPradelna, 1800)}, null));
        lst.addActionBinding(new ActionBinding(zadveriDoldePradelnaSw.getLeftBottomButton(), new Action[]{new SwitchOffAction(svPradelna)}, null));
        lst.addActionBinding(new ActionBinding(zadveriDoldePradelnaSw.getRightUpperButton(), new Action[]{new SwitchOnSensorAction(svSklepLevy, 1800)}, null));
        lst.addActionBinding(new ActionBinding(zadveriDoldePradelnaSw.getRightBottomButton(), new Action[]{new SwitchOffAction(svSklepLevy)}, null));

        configurePwmLights(lst, zadveriDoleChodbaSw, WallSwitch.Side.RIGHT, 40, zadveriDolePwmActor);
        configurePwmLights(lst, zadveriDoleChodbaSw, WallSwitch.Side.LEFT, 40, chodbaDolePwmActor);

        // chodba dole
        lst.addActionBinding(new ActionBinding(chodbaDoleSpajzSw3.getLeftUpperButton(), new Action[]{new SwitchOnSensorAction(svSpajza, 1800)}, null));
        lst.addActionBinding(new ActionBinding(chodbaDoleSpajzSw3.getLeftBottomButton(), new Action[]{new SwitchOffAction(svSpajza)}, null));
        configurePwmLights(lst, chodbaDoleSpajzSw3, WallSwitch.Side.RIGHT, 40, chodbaDolePwmActor);

        // pradelna
        lst.addActionBinding(new ActionBinding(pradelnaSw1.getLeftUpperButton(), new Action[]{new SwitchOnSensorAction(svPradelna, 1800)}, null));
        lst.addActionBinding(new ActionBinding(pradelnaSw1.getLeftBottomButton(), new Action[]{new SwitchOffAction(svPradelna)}, null));
        lst.addActionBinding(new ActionBinding(pradelnaSw1.getRightUpperButton(), new Action[]{new SwitchOnSensorAction(svPradelna, 1800)}, null));
        lst.addActionBinding(new ActionBinding(pradelnaSw1.getRightBottomButton(), new Action[]{new SwitchOffAction(svPradelna)}, null));

        // PIRs
        InputDevice pirA1Prizemi = new InputDevice("pirA1Prizemi", pirNodeA, 1);
        setupPir(lst, pirA1Prizemi.getIn1AndActivate(), "Pradelna dvere", new SwitchOnSensorAction(svPradelna, 600), new SwitchOffSensorAction(svPradelna, 60));
        setupPir(lst, pirA1Prizemi.getIn2AndActivate(), "Pradelna pracka", new SwitchOnSensorAction(svPradelna, 600), new SwitchOffSensorAction(svPradelna, 60));

        InputDevice pirA2Patro = new InputDevice("pirA2Patro", pirNodeA, 2);
        setupPir(lst, pirA2Patro.getIn1AndActivate(), "Chodba pred WC", (Action) null, null);
        setupPir(lst, pirA2Patro.getIn2AndActivate(), "Chodba", (Action) null, null);
        setupPir(lst, pirA2Patro.getIn3AndActivate(), "WC", new SwitchOnSensorAction(wcPwmActor, 600, 0, -15), new SwitchOffSensorAction(wcPwmActor, 60));
        setupPir(lst, pirA2Patro.getIn5AndActivate(), "Zadveri hore vchod", new SwitchOnSensorAction(zadveriPwmActor, 600, 0, -15), new SwitchOffSensorAction(zadveriPwmActor, 15));
        setupPir(lst, pirA2Patro.getIn6AndActivate(), "Zadveri hore chodba", new SwitchOnSensorAction(zadveriPwmActor, 600, 0, -15), new SwitchOffSensorAction(zadveriPwmActor, 15));
        setupPir(lst, pirA2Patro.getIn4AndActivate(), "Chodba nad Markem", new SwitchOnSensorAction(satnaPwmActor, 600, 0, -15), new SwitchOffSensorAction(satnaPwmActor, 60));

        InputDevice pirA3Prizemi = new InputDevice("pirA3Prizemi", pirNodeA, 3);
        setupPir(lst, pirA3Prizemi.getIn1AndActivate(), "Jidelna", (Action) null, null);
        setupPir(lst, pirA3Prizemi.getIn2AndActivate(), "Obyvak", (Action) null, null);
        setupPir(lst, pirA3Prizemi.getIn3AndActivate(), "Chodba dole", new SwitchOnSensorAction(chodbaDolePwmActor, 600, 0, -15), new SwitchOffSensorAction(chodbaDolePwmActor, 15));
        setupPir(lst, pirA3Prizemi.getIn4AndActivate(), "Koupelna dole", (Action) null, null);
        setupPir(lst, pirA3Prizemi.getIn5AndActivate(), "Spajza", new SwitchOnSensorAction(svSpajza, 600), new SwitchOffSensorAction(svSpajza, 20));
        setupPir(lst, pirA3Prizemi.getIn6AndActivate(), "Zadveri dole", new SwitchOnSensorAction(zadveriDolePwmActor, 600, -15, -30), new SwitchOffSensorAction(zadveriDolePwmActor, 15));

//        Servlet.action1 = onActionKoupelna;
//        Servlet.action2 = offActionKoupelna;
        Servlet.action3 = invertJidelna;

        Servlet.action4 = null;
        Servlet.action5 = null;
        Servlet.louversActions = louversInvertActions;

        //test wall switch application
        WallSwitch testSw = new WallSwitch("testSwA", testNode20, 1);
        TestingOnOffActor testingRightOnOffActor = new TestingOnOffActor("RightSwitchTestingActor", null, 0, 1, testSw.getRedLedIndicator(true));
        TestingOnOffActor testingLeftOnOffActor = new TestingOnOffActor("LeftSwitchTestingActor", null, 0, 1, testSw.getGreenLedIndicator(true));
        lst.addActionBinding(new ActionBinding(testSw.getRightBottomButton(), new Action[]{new SwitchOffAction(testingRightOnOffActor)}, null));
        lst.addActionBinding(new ActionBinding(testSw.getRightUpperButton(), new Action[]{new SwitchOnAction(testingRightOnOffActor)}, null));
        lst.addActionBinding(new ActionBinding(testSw.getLeftUpperButton(), new Action[]{new SwitchOnAction(testingLeftOnOffActor)}, null));
        lst.addActionBinding(new ActionBinding(testSw.getLeftBottomButton(), new Action[]{new SwitchOffAction(testingLeftOnOffActor)}, null));

        Servlet.lightsActions = lightsActions.toArray(new Action[lightsActions.size()]);
        Servlet.pirStatusList = pirStatusList;
//        OnOffActor testLedActor = new OnOffActor("testLed", testOutputDevice3.getOut2(), 1, 0);
//        lst.addActionBinding(new ActionBinding(testInputDevice2.getIn1(), new Action[]{new SensorAction(testLedActor, 10)}, new Action[]{new SensorAction(testLedActor, 60)}));
    }

    private static void setupPir(SwitchListener lst, NodePin pirPin, String name, Action activateAction, Action deactivateAction) {
        PirStatus status = new PirStatus(name);
        Action[] activateActions = (activateAction != null) ? new Action[]{activateAction, status.getActivateAction()} : new Action[]{status.getActivateAction()};
        Action[] deactivateActions = (deactivateAction != null) ? new Action[]{deactivateAction, status.getDeactivateAction()} : new Action[]{status.getDeactivateAction()};
        setupPir(lst, pirPin, name, activateActions, deactivateActions);
        pirStatusList.add(status);
    }

    private static void setupPir(SwitchListener lst, NodePin pirPin, String name, Action[] activateActions, Action[] deactivateActions) {
        lst.addActionBinding(new ActionBinding(pirPin, deactivateActions, activateActions));
    }

    private static void configurePwmLights(SwitchListener lst, WallSwitch wallSwitch, WallSwitch.Side side, int initialPwmValue, PwmActor... pwmActors) {
        Action[][] actions = new Action[4][pwmActors.length];
        for (int i = 0; i < pwmActors.length; i++) {
            PwmActionGroup actionGroup = new PwmActionGroup(pwmActors[i], initialPwmValue);
            actions[0][i] = actionGroup.getUpButtonDownAction();
            actions[1][i] = actionGroup.getUpButtonUpAction();
            actions[2][i] = actionGroup.getDownButtonDownAction();
            actions[3][i] = actionGroup.getDownButtonUpAction();

        }
        NodePin upperButton = getUpperButton(wallSwitch, side);
        NodePin bottomButton = getBottomButton(wallSwitch, side);
        lst.addActionBinding(new ActionBinding(upperButton, actions[0], actions[1]));
        lst.addActionBinding(new ActionBinding(bottomButton, actions[2], actions[3]));
    }

    static PwmActor addLddLight(ArrayList<Action> lightsActions, String name, LddBoardDevice.LddNodePin pin, double maxLoad, Indicator... indicators) {
        PwmActor pwmActor = new PwmActor(name, pin, maxLoad / pin.getMaxLddCurrent(), indicators);
        lightsActions.add(new SwitchOnAction(pwmActor));
        lightsActions.add(new IncreasePwmAction(pwmActor));
        lightsActions.add(new DecreasePwmAction(pwmActor));
        lightsActions.add(new SwitchOffAction(pwmActor));
        return pwmActor;
    }

    static void configureLouvers(SwitchListener lst, WallSwitch wallSwitch, WallSwitch.Side side, OnOffActor louversUp, OnOffActor louversDown, int duration) {
        NodePin upTrigger = getUpperButton(wallSwitch, side);
        NodePin downTrigger = getBottomButton(wallSwitch, side);

        lst.addActionBinding(new ActionBinding(upTrigger, new Action[]{new InvertActionWithTimer(louversUp, duration)}, null));
        lst.addActionBinding(new ActionBinding(downTrigger, new Action[]{new InvertActionWithTimer(louversDown, duration)}, null));
    }

    private static NodePin getBottomButton(WallSwitch wallSwitch, WallSwitch.Side side) {
        return (side == WallSwitch.Side.LEFT) ? wallSwitch.getLeftBottomButton() : wallSwitch.getRightBottomButton();
    }

    private static NodePin getUpperButton(WallSwitch wallSwitch, WallSwitch.Side side) {
        return (side == WallSwitch.Side.LEFT) ? wallSwitch.getLeftUpperButton() : wallSwitch.getRightUpperButton();
    }

    static void configureLouvers(SwitchListener lst, WallSwitch.Side side, WallSwitch wallSwitch, OnOffActor louvers1Up, OnOffActor louvers1Down, OnOffActor louvers2Up, OnOffActor louvers2Down, int duration) {
        NodePin upTrigger = getUpperButton(wallSwitch, side);
        NodePin downTrigger = getBottomButton(wallSwitch, side);

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
