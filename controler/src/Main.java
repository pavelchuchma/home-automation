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
        Node chodbaDole = nodeInfoCollector.createNode(8, "ChodbaDole");
        Node koupelnaHore = nodeInfoCollector.createNode(9, "KoupelnaHore");
        Node vratnice = nodeInfoCollector.createNode(10, "Vratnice");
        Node obyvakSpinacABC = nodeInfoCollector.createNode(11, "ObyvakSpinacABC");
        Node krystof = nodeInfoCollector.createNode(12, "Krystof");
        Node zaluzieA = nodeInfoCollector.createNode(13, "ZaluzieA");
        Node marek = nodeInfoCollector.createNode(14, "Marek");
        Node chodbaA = nodeInfoCollector.createNode(16, "ChodbaA");
        Node lddActorA = nodeInfoCollector.createNode(19, "LDD-ActorA");
        Node testNode20 = nodeInfoCollector.createNode(20, "TestNode20");
        Node lozniceDvere = nodeInfoCollector.createNode(21, "LozniceDvere");

        WallSwitch chodbaA1Sw = new WallSwitch("chodbaA1Sw", chodbaA, 1);
        WallSwitch chodbaA2Sw = new WallSwitch("chodbaA2Sw", chodbaA, 2);

        WallSwitch obyvakA1Sw = new WallSwitch("obyvakA1Sw", obyvakSpinacABC, 1);
        WallSwitch obyvakA2Sw = new WallSwitch("obyvakA2Sw", obyvakSpinacABC, 2);
        WallSwitch obyvakA3Sw = new WallSwitch("obyvakA3Sw", obyvakSpinacABC, 3);
        OutputDevice triak1Actor3Port3 = new OutputDevice("triak1Actor3Port3", actor3, 3);
        RelayBoardDevice rele1Actor3Port2 = new RelayBoardDevice("rele1Actor3Port2", actor3, 2);

        WallSwitch zadveriSwA1 = new WallSwitch("zadveriSwA1", zadveri, 1);

        WallSwitch koupelnaHoreSw1 = new WallSwitch("koupelnaHoreSw1", koupelnaHore, 1);
        WallSwitch koupelnaHoreSw2 = new WallSwitch("koupelnaHoreSw2", koupelnaHore, 2);
        WallSwitch chodbaDoldeSwA = new WallSwitch("chodbaDoldeSwA", chodbaDole, 1);
        WallSwitch lozniceOknoSw1 = new WallSwitch("lozniceOknoSw1", lozniceOkno, 1);
        WallSwitch lozniceDvereSw1 = new WallSwitch("lozniceDvereSw1", lozniceDvere, 1);
        WallSwitch lozniceDvereSw2 = new WallSwitch("lozniceDvereSw2", lozniceDvere, 2);
        WallSwitch marekPostelSw3 = new WallSwitch("marekPostelSw3", lozniceDvere, 3);
        WallSwitch vratniceSw1 = new WallSwitch("vratniceSw1", vratnice, 1);
        WallSwitch vratniceSw2 = new WallSwitch("vratniceSw2", vratnice, 2);
        WallSwitch krystofSwA1 = new WallSwitch("krystofSwA1", krystof, 1);
        WallSwitch krystofSwA2 = new WallSwitch("krystofSwA2", krystof, 2);
        WallSwitch marekSwA1 = new WallSwitch("marekSwA1", marek, 1);

        OnOffActor svKoupelna = new OnOffActor("svKoupelna", triak1Actor3Port3.getOut1(), 1, 0, obyvakA3Sw.getGreenLedIndicator(false), koupelnaHoreSw1.getGreenLedIndicator(false), koupelnaHoreSw1.getRedLedIndicator(true));
        OnOffActor svJidelna = new OnOffActor("svJidelna", triak1Actor3Port3.getOut2(), 1, 0, obyvakA3Sw.getRedLedIndicator(true));
        OnOffActor svSpajza = new OnOffActor("svSpajza", triak1Actor3Port3.getOut3(), 1, 0);
        OnOffActor svPradelna = new OnOffActor("svPradelna", triak1Actor3Port3.getOut4(), 1, 0, chodbaDoldeSwA.getGreenLedIndicator(false), chodbaDoldeSwA.getRedLedIndicator(true));
        OnOffActor zaricKoupelnaHore2Trubice = new OnOffActor("zaricKoupelnaHore2Trubice", rele1Actor3Port2.getRele1(), 0, 1, koupelnaHoreSw2.getRedLedIndicator(true));
        OnOffActor zaricKoupelnaHore1Trubice = new OnOffActor("zaricKoupelnaHore1Trubice", rele1Actor3Port2.getRele2(), 0, 1, koupelnaHoreSw2.getGreenLedIndicator(true));

        SwitchListener lst = nodeInfoCollector.getSwitchListener();

        Action invertJidelna = new InvertAction(svJidelna);
        lst.addActionBinding(new ActionBinding(obyvakA3Sw.getRightBottomButton(), new Action[]{new InvertAction(svKoupelna)}, null));
        lst.addActionBinding(new ActionBinding(obyvakA3Sw.getRightUpperButton(), new Action[]{invertJidelna}, null));

        Action onActionKoupelna = new SwitchOnAction(svKoupelna);
        Action offActionKoupelna = new SwitchOffAction(svKoupelna);
        lst.addActionBinding(new ActionBinding(koupelnaHoreSw1.getRightBottomButton(), new Action[]{offActionKoupelna}, null));
        lst.addActionBinding(new ActionBinding(koupelnaHoreSw1.getRightUpperButton(), new Action[]{onActionKoupelna}, null));

        Action onActionPradelna = new SwitchOnAction(svPradelna);
        Action offActionPradelna = new SwitchOffAction(svPradelna);
        lst.addActionBinding(new ActionBinding(chodbaDoldeSwA.getRightBottomButton(), new Action[]{offActionPradelna}, null));
        lst.addActionBinding(new ActionBinding(chodbaDoldeSwA.getRightUpperButton(), new Action[]{onActionPradelna}, null));
        lst.addActionBinding(new ActionBinding(chodbaDoldeSwA.getLeftUpperButton(), new Action[]{onActionPradelna}, null));
        lst.addActionBinding(new ActionBinding(chodbaDoldeSwA.getLeftBottomButton(), new Action[]{offActionPradelna}, null));

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
        PwmActor marekPwmActor = addLddLight(lightsActions, "Marek", lddDevice1.getLdd1(), 0.95, new SwitchIndicator(marekSwA1.getRedLed(), true), new SwitchIndicator(marekPostelSw3.getRedLed(), true));
        PwmActor pataPwmActor = addLddLight(lightsActions, "Paťa", lddDevice1.getLdd2(), 0.95);
        PwmActor krystofPwmActor = addLddLight(lightsActions, "Kryštof", lddDevice1.getLdd3(), 0.95, new SwitchIndicator(krystofSwA2.getRedLed(), true));
        PwmActor zadveriPwmActor = addLddLight(lightsActions, "Zádveří", lddDevice1.getLdd4(), 0.30, new SwitchIndicator(zadveriSwA1.getRedLed(), true)); //0.48
        PwmActor loznice1PwmActor = addLddLight(lightsActions, "Ložnice velké", lddDevice1.getLdd5(), 1.0);
        PwmActor satnaPwmActor = addLddLight(lightsActions, "Šatna", lddDevice1.getLdd6(), 0.30, new SwitchIndicator(chodbaA1Sw.getRedLed(), true)); //0.48

        LddBoardDevice lddDeviceTest = new LddBoardDevice("lddTestDevice1", lddActorA, 2, 1.0, .7, .7, .7, .7, .35);
        PwmActor vratnice1PwmActor = addLddLight(lightsActions, "Vrátnice 1", lddDeviceTest.getLdd1(), 0.95, new SwitchIndicator(vratniceSw1.getRedLed(), true));
        PwmActor vratnice2PwmActor = addLddLight(lightsActions, "Vrátnice 2", lddDeviceTest.getLdd2(), 0.48);
        PwmActor loznice2PwmActor = addLddLight(lightsActions, "Ložnice malé", lddDeviceTest.getLdd3(), 0.35); // .36
        addLddLight(lightsActions, "Ldd-test2.4", lddDeviceTest.getLdd4(), 0.05);
        addLddLight(lightsActions, "Ldd-test2.5", lddDeviceTest.getLdd5(), 0.05);
        PwmActor wcPwmActor = addLddLight(lightsActions, "WC", lddDeviceTest.getLdd6(), 0.24);

        Action[] louversInvertActions = new Action[louversActors.length];
        for (int i = 0; i < louversActors.length; i++) {
            louversInvertActions[i] = new InvertActionWithTimer(louversActors[i], 70);
            if (i % 2 == 1) {
                louversActors[i].setConflictingActor(louversActors[i - 1]);
                louversActors[i - 1].setConflictingActor(louversActors[i]);
            }
        }

        // koupelna
        configureLouvers(lst, WallSwitch.Side.LEFT, koupelnaHoreSw1, zaluzieKoupelnaUp, zaluzieKoupelnaDown, 50);

        // kuchyn + obyvak
        configureLouvers(lst, WallSwitch.Side.LEFT, obyvakA1Sw, zaluzieKuchynUp, zaluzieKuchynDown, 70);
        configureLouvers(lst, WallSwitch.Side.RIGHT, obyvakA1Sw, zaluzieObyvak1Up, zaluzieObyvak1Down, 70);
        configureLouvers(lst, WallSwitch.Side.LEFT, obyvakA2Sw, zaluzieObyvak2Up, zaluzieObyvak2Down, zaluzieObyvak3Up, zaluzieObyvak3Down, 70);
        configureLouvers(lst, WallSwitch.Side.RIGHT, obyvakA2Sw, zaluzieObyvak4Up, zaluzieObyvak4Down, 70);
        configureLouvers(lst, WallSwitch.Side.LEFT, obyvakA3Sw, zaluzieObyvak5Up, zaluzieObyvak5Down, zaluzieObyvak6Up, zaluzieObyvak6Down, 70);


        // chodba
        // svetla satna
        configurePwmLights(lst, chodbaA1Sw, WallSwitch.Side.LEFT, 80, satnaPwmActor);
        configureLouvers(lst, WallSwitch.Side.RIGHT, chodbaA1Sw, zaluzieSatnaUp, zaluzieSatnaDown, 50);

        // zadveri
        configurePwmLights(lst, zadveriSwA1, WallSwitch.Side.LEFT, 80, zadveriPwmActor);
        configurePwmLights(lst, zadveriSwA1, WallSwitch.Side.RIGHT, 80, zadveriPwmActor);

        // Krystof + Pata
        configureLouvers(lst, WallSwitch.Side.LEFT, krystofSwA1, zaluziePataUp, zaluziePataDown, 50);
        configureLouvers(lst, WallSwitch.Side.RIGHT, krystofSwA1, zaluzieKrystofUp, zaluzieKrystofDown, 50);
        configurePwmLights(lst, krystofSwA2, WallSwitch.Side.LEFT, 50, pataPwmActor);
        configurePwmLights(lst, krystofSwA2, WallSwitch.Side.RIGHT, 50, krystofPwmActor);

        // Marek
        configureLouvers(lst, WallSwitch.Side.LEFT, marekSwA1, zaluzieMarekUp, zaluzieMarekDown, 50);
        configurePwmLights(lst, marekSwA1, WallSwitch.Side.RIGHT, 50, marekPwmActor);
        configureLouvers(lst, WallSwitch.Side.LEFT, marekPostelSw3, zaluzieMarekUp, zaluzieMarekDown, 50);
        configurePwmLights(lst, marekPostelSw3, WallSwitch.Side.RIGHT, 50, marekPwmActor);


        // chodba
        configureLouvers(lst, WallSwitch.Side.LEFT, chodbaA2Sw, zaluzieChodba1Up, zaluzieChodba1Down, 50);
        configureLouvers(lst, WallSwitch.Side.RIGHT, chodbaA2Sw, zaluzieChodba2Up, zaluzieChodba2Down, 50);

        // loznice
        configureLouvers(lst, WallSwitch.Side.LEFT, lozniceOknoSw1, zaluzieLoznice1Up, zaluzieLoznice1Down, 40);
        configureLouvers(lst, WallSwitch.Side.RIGHT, lozniceOknoSw1, zaluzieLoznice2Up, zaluzieLoznice2Down, 40);
        configureLouvers(lst, WallSwitch.Side.LEFT, lozniceDvereSw1, zaluzieLoznice1Up, zaluzieLoznice1Down, 40);
        configureLouvers(lst, WallSwitch.Side.RIGHT, lozniceDvereSw1, zaluzieLoznice2Up, zaluzieLoznice2Down, 40);
        configurePwmLights(lst, lozniceDvereSw2, WallSwitch.Side.LEFT, 40, loznice2PwmActor);
        configurePwmLights(lst, lozniceDvereSw2, WallSwitch.Side.RIGHT, 40, loznice1PwmActor);

        // vratnice
        configureLouvers(lst, WallSwitch.Side.RIGHT, vratniceSw1, zaluzieVratnice3Up, zaluzieVratnice3Down, 50);
        configureLouvers(lst, WallSwitch.Side.LEFT, vratniceSw2, zaluzieVratnice2Up, zaluzieVratnice2Down, 40);
        configureLouvers(lst, WallSwitch.Side.RIGHT, vratniceSw2, zaluzieVratnice1Up, zaluzieVratnice1Down, 40);
        configurePwmLights(lst, vratniceSw1, WallSwitch.Side.LEFT, 40, vratnice1PwmActor, vratnice2PwmActor);

        // PIRs
        InputDevice pirA1Prizemi = new InputDevice("pirA1Prizemi", pirNodeA, 1);
        lst.addActionBinding(new ActionBinding(pirA1Prizemi.getIn1AndActivate(), new Action[]{new SwitchOffSensorAction(svPradelna, 10)}, new Action[]{new SwitchOnSensorAction(svPradelna, 600)}));
        lst.addActionBinding(new ActionBinding(pirA1Prizemi.getIn2AndActivate(), new Action[]{new SwitchOffSensorAction(svPradelna, 10)}, new Action[]{new SwitchOnSensorAction(svPradelna, 600)}));

        InputDevice pirA2Patro = new InputDevice("pirA2Patro", pirNodeA, 2);
        lst.addActionBinding(new ActionBinding(pirA2Patro.getIn3AndActivate(), new Action[]{new SwitchOffSensorAction(wcPwmActor, 60)}, new Action[]{new SwitchOnSensorAction(wcPwmActor, 600, 0, -15)}));
        lst.addActionBinding(new ActionBinding(pirA2Patro.getIn5AndActivate(), new Action[]{new SwitchOffSensorAction(zadveriPwmActor, 15)}, new Action[]{new SwitchOnSensorAction(zadveriPwmActor, 600, 0, -15)}));
        lst.addActionBinding(new ActionBinding(pirA2Patro.getIn6AndActivate(), new Action[]{new SwitchOffSensorAction(zadveriPwmActor, 15)}, new Action[]{new SwitchOnSensorAction(zadveriPwmActor, 600, 0, -15)}));
        lst.addActionBinding(new ActionBinding(pirA2Patro.getIn4AndActivate(), new Action[]{new SwitchOffSensorAction(satnaPwmActor, 60)}, new Action[]{new SwitchOnSensorAction(satnaPwmActor, 600, 0, -15)}));

        InputDevice pirA3Prizemi = new InputDevice("pirA3Prizemi", pirNodeA, 3);
        lst.addActionBinding(new ActionBinding(pirA3Prizemi.getIn5AndActivate(), new Action[]{new SwitchOffSensorAction(svSpajza, 10)}, new Action[]{new SwitchOnSensorAction(svSpajza, 600)}));


        Servlet.action1 = onActionKoupelna;
        Servlet.action2 = offActionKoupelna;
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

//        OnOffActor testLedActor = new OnOffActor("testLed", testOutputDevice3.getOut2(), 1, 0);
//        lst.addActionBinding(new ActionBinding(testInputDevice2.getIn1(), new Action[]{new SensorAction(testLedActor, 10)}, new Action[]{new SensorAction(testLedActor, 60)}));


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

    static void configureLouvers(SwitchListener lst, WallSwitch.Side side, WallSwitch wallSwitch, OnOffActor louversUp, OnOffActor louversDown, int duration) {
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
