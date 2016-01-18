import app.NodeInfoCollector;
import app.SwitchListener;
import controller.ActionBinding;
import controller.PirStatus;
import controller.action.AbstractSensorAction;
import controller.action.Action;
import controller.action.DecreasePwmAction;
import controller.action.IncreasePwmAction;
import controller.action.InvertAction;
import controller.action.InvertActionWithTimer;
import controller.action.LouversActionGroup;
import controller.action.PwmActionGroup;
import controller.action.SwitchOffAction;
import controller.action.SwitchOffSensorAction;
import controller.action.SwitchOnAction;
import controller.action.SwitchOnSensorAction;
import controller.actor.IOnOffActor;
import controller.actor.Indicator;
import controller.actor.OnOffActor;
import controller.actor.PwmActor;
import controller.actor.TestingOnOffActor;
import controller.controller.LouversController;
import controller.controller.LouversControllerImpl;
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
    static List<PirStatus> pirStatusList = new ArrayList<>();

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
        Node schodyDoleL = nodeInfoCollector.createNode(5, "SchodyDoleL");
        Node lozniceOkno = nodeInfoCollector.createNode(6, "LozniceOkno");
        Node pirNodeA = nodeInfoCollector.createNode(7, "PirNodeA");
        Node zadveriDoleChodba = nodeInfoCollector.createNode(8, "ZadveriDoleUChodby");
        Node koupelnaHore = nodeInfoCollector.createNode(9, "KoupelnaHore");
        Node vratnice = nodeInfoCollector.createNode(10, "Vratnice");
        Node schodyDoleR = nodeInfoCollector.createNode(11, "SchodyDoleR");
        Node krystof = nodeInfoCollector.createNode(12, "Krystof");
        Node zaluzieA = nodeInfoCollector.createNode(13, "ZaluzieA");
        Node marek = nodeInfoCollector.createNode(14, "Marek");
        Node patrik = nodeInfoCollector.createNode(15, "Patrik");
        Node chodbaA = nodeInfoCollector.createNode(16, "ChodbaA");
        Node zadveriDoleVchod = nodeInfoCollector.createNode(17, "ZadveriDoleUVchodu");
        Node pracovna = nodeInfoCollector.createNode(18, "Pracovna");
        Node lddActorA = nodeInfoCollector.createNode(19, "LDD-ActorA");
        Node garazVzadu = nodeInfoCollector.createNode(20, "GarazVzadu");
        Node lozniceDvere = nodeInfoCollector.createNode(21, "LozniceDvere");
        Node garazVpredu = nodeInfoCollector.createNode(22, "GarazVpredu");
        Node pradelna = nodeInfoCollector.createNode(23, "Pradelna");
        Node lddActorB = nodeInfoCollector.createNode(24, "LDD-ActorB");
        Node koupelnaDole = nodeInfoCollector.createNode(25, "KoupelnaDole");
        Node kuchyn = nodeInfoCollector.createNode(26, "KuchynDole");
        Node obyvakGauc = nodeInfoCollector.createNode(27, "ObyvakGauc");
        Node obyvakVzaduL = nodeInfoCollector.createNode(28, "ObyvakVzaduL");
        Node lddActorC = nodeInfoCollector.createNode(29, "LDD-ActorC");
        Node obyvakVzaduR = nodeInfoCollector.createNode(30, "ObyvakVzaduR");
        Node sklep = nodeInfoCollector.createNode(31, "Sklep");
        Node switchTestNode50 = nodeInfoCollector.createNode(50, "SwitchTestNode50");
        Node switchTestNode41 = nodeInfoCollector.createNode(41, "SwitchTestNode41");

        WallSwitch chodbaALSw = new WallSwitch("chodbaALSw", chodbaA, 1);
        WallSwitch chodbaARSw = new WallSwitch("chodbaARSw", chodbaA, 2);
        WallSwitch wcSw = new WallSwitch("wcSw", chodbaA, 3);

        WallSwitch schodyDoleL1Sw = new WallSwitch("schodyDoleL1Sw", schodyDoleL, 1);
        WallSwitch schodyDoleL2Sw = new WallSwitch("schodyDoleL2Sw", schodyDoleL, 2);
        WallSwitch schodyDoleR1Sw = new WallSwitch("schodyDoleR1Sw", schodyDoleR, 1);
        WallSwitch schodyDoleR2Sw = new WallSwitch("schodyDoleR2Sw", schodyDoleR, 2);
        WallSwitch schodyDoleR3Sw = new WallSwitch("schodyDoleR3Sw", schodyDoleR, 3);

        WallSwitch obyvakVzaduL1Sw = new WallSwitch("ObyvakVzaduL1Sw", obyvakVzaduL, 1);
        WallSwitch obyvakVzaduL2Sw = new WallSwitch("ObyvakVzaduL2Sw", obyvakVzaduL, 2);
        WallSwitch obyvakVzaduR1Sw = new WallSwitch("ObyvakVzaduR1Sw", obyvakVzaduR, 1);
        WallSwitch obyvakVzaduR2Sw = new WallSwitch("ObyvakVzaduR2Sw", obyvakVzaduR, 2);
        WallSwitch obyvakVzaduR3Sw = new WallSwitch("ObyvakVzaduR3Sw", obyvakVzaduR, 3);

        WallSwitch obyvakGaucLSw = new WallSwitch("ObyvakGaucLSw", obyvakGauc, 1);
        WallSwitch obyvakGaucRSw = new WallSwitch("ObyvakGaucRSw", obyvakGauc, 2);

        WallSwitch sklepLevyLSw = new WallSwitch("sklepLevyLSw", sklep, 1);
        WallSwitch sklepLevyRSw = new WallSwitch("sklepLevyRSw", sklep, 3);
        WallSwitch sklepPravySw = new WallSwitch("sklepPravySw", sklep, 2);

        OutputDevice triak1Actor3Port3 = new OutputDevice("triak1Actor3Port3", actor3, 3);
        RelayBoardDevice rele1Actor3Port2 = new RelayBoardDevice("rele1Actor3Port2", actor3, 2);

        WallSwitch zadveriSwA1 = new WallSwitch("zadveriSwA1", zadveri, 1);
        WallSwitch zadveriSwA2 = new WallSwitch("zadveriSwA2", zadveri, 2);

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
        WallSwitch chodbaHoreKrystofSwA3 = new WallSwitch("chodbaHoreKrystofSwA3", krystof, 3);
        WallSwitch marekSwA1 = new WallSwitch("marekSwA1", marek, 1);
        WallSwitch patrikPostelSw3 = new WallSwitch("patrikPostelSw3", marek, 3);
        WallSwitch krystofPostelSw = new WallSwitch("krystofPostel", zadveriDoleChodba, 2);
        WallSwitch patrikSw1 = new WallSwitch("pataSw1", patrik, 1);
        WallSwitch chodbaHorePatrikSw3 = new WallSwitch("chodbaHorePataSw3", patrik, 3);
        WallSwitch pracovnaSw2 = new WallSwitch("pracovnaSw2", pracovna, 2);
        WallSwitch satnaSw3 = new WallSwitch("satnaSw3", pracovna, 3);
        WallSwitch zadveriDoleVchodLSw = new WallSwitch("zadveriDoleVchodLSw", zadveriDoleVchod, 1);
        WallSwitch zadveriDoleVchodRSw = new WallSwitch("zadveriDoleVchodRSw", zadveriDoleVchod, 2);
        WallSwitch koupelnaHoreOknoSw = new WallSwitch("koupelnaHoreOknoSw", zadveriDoleVchod, 3);
        WallSwitch garazASw1 = new WallSwitch("garazASw1", garazVpredu, 1);
        WallSwitch garazASw2 = new WallSwitch("garazASw2", garazVpredu, 2);
        WallSwitch garazBSwL = new WallSwitch("garazBSwL", garazVzadu, 1);
        WallSwitch garazBSwR = new WallSwitch("garazBSwL", garazVzadu, 2);
        WallSwitch pradelnaSw1 = new WallSwitch("pradelnaSw1", pradelna, 1);
        WallSwitch chodbaDoleSpajzSw3 = new WallSwitch("chodbaDoleSpajzSw3", pradelna, 3);
        WallSwitch koupelnaDoleSw1 = new WallSwitch("koupelnaDoleSw1", koupelnaDole, 1);
        WallSwitch koupelnaDoleSw2 = new WallSwitch("koupelnaDoleSw2", koupelnaDole, 2);
        WallSwitch kuchynSw1 = new WallSwitch("kuchynSw1", kuchyn, 1);
        WallSwitch kuchynSw2 = new WallSwitch("kuchynSw2", kuchyn, 2);

        OnOffActor svJidelna = new OnOffActor("svJidelna", triak1Actor3Port3.getOut1(), 1, 0, schodyDoleR3Sw.getRedLedIndicator(true));
        OnOffActor svSklepLevy = new OnOffActor("svSklepLevy", triak1Actor3Port3.getOut2(), 1, 0, sklepLevyLSw.getRedLedIndicator(false), zadveriDoleVchodRSw.getGreenLedIndicator(false));
        OnOffActor svSklepPravy = new OnOffActor("svSklepPravy", triak1Actor3Port3.getOut5(), 1, 0, sklepLevyRSw.getRedLedIndicator(false), zadveriDoleVchodRSw.getRedLedIndicator(false));
        OnOffActor svSpajza = new OnOffActor("svSpajza", triak1Actor3Port3.getOut3(), 1, 0, chodbaDoleSpajzSw3.getRedLedIndicator(false));
        OnOffActor zasStromek = new OnOffActor("zasStromek", triak1Actor3Port3.getOut4(), 1, 0, zadveriSwA1.getGreenLedIndicator(false));
        OnOffActor zaricKoupelnaHore2Trubice = new OnOffActor("zaricKoupelnaHore2Trubice", rele1Actor3Port2.getRele1(), 0, 1, koupelnaHoreSw2.getRedLedIndicator(true), koupelnaHoreOknoSw.getRedLedIndicator(true));
        OnOffActor zaricKoupelnaHore1Trubice = new OnOffActor("zaricKoupelnaHore1Trubice", rele1Actor3Port2.getRele2(), 0, 1, koupelnaHoreSw2.getGreenLedIndicator(true), koupelnaHoreOknoSw.getGreenLedIndicator(true));

        RelayBoardDevice rele9 = new RelayBoardDevice("rele9", lddActorB, 3);
        OnOffActor ovladacGaraz = new OnOffActor("ovladacGaraz", rele9.getRele2(), 0, 1);
        OnOffActor bzucakDvere = new OnOffActor("bzucakDvere", rele9.getRele1(), 0, 1);

        SwitchListener lst = nodeInfoCollector.getSwitchListener();

        Action invertJidelna = new InvertAction(svJidelna);
        lst.addActionBinding(new ActionBinding(schodyDoleR3Sw.getRightUpperButton(), new Action[]{invertJidelna}, null));

        // zaluzie
        RelayBoardDevice rele3ZaluzieAPort1 = new RelayBoardDevice("rele3ZaluzieAPort1", zaluzieA, 1);
        RelayBoardDevice rele4ZaluzieAPort2 = new RelayBoardDevice("rele3ZaluzieAPort1", zaluzieA, 2);
        RelayBoardDevice rele2ZaluzieAPort3 = new RelayBoardDevice("rele3ZaluzieAPort1", zaluzieA, 3);

        RelayBoardDevice rele6ZaluzieBPort1 = new RelayBoardDevice("rele6ZaluzieBPort1", zaluzieB, 1);
        RelayBoardDevice rele5ZaluzieBPort2 = new RelayBoardDevice("rele5ZaluzieBPort2", zaluzieB, 2);
        RelayBoardDevice rele7ZaluzieBPort3 = new RelayBoardDevice("rele7ZaluzieBPort3", zaluzieB, 3);

        RelayBoardDevice rele8Actor3Port1 = new RelayBoardDevice("rele8Actor3Port1", actor3, 1);


        LouversController zaluzieKrystof;
        LouversController zaluziePata;
        LouversController zaluzieMarek;
        LouversController zaluzieKoupelna;
        LouversController zaluzieLoznice1;
        LouversController zaluzieLoznice2;
        LouversController zaluzieSatna;
        LouversController zaluziePracovna;
        LouversController zaluzieKuchyn;
        LouversController zaluzieObyvak1;
        LouversController zaluzieObyvak2;
        LouversController zaluzieObyvak3;
        LouversController zaluzieObyvak4;
        LouversController zaluzieObyvak5;
        LouversController zaluzieObyvak6;
        LouversController zaluzieChodba1;
        LouversController zaluzieChodba2;
        LouversController zaluzieVratnice1;
        LouversController zaluzieVratnice2;
        LouversController zaluzieVratnice3;
        LouversController zaluzieKoupelnaDole;

        LouversController[] louversControllers = new LouversController[]{
                zaluzieKoupelna = new LouversControllerImpl("Koupelna", rele6ZaluzieBPort1.getRele1(), rele6ZaluzieBPort1.getRele2(), 39000, 1600),
                zaluzieKrystof = new LouversControllerImpl("Kryštof", rele3ZaluzieAPort1.getRele1(), rele3ZaluzieAPort1.getRele2(), 35000, 1600),
                zaluziePata = new LouversControllerImpl("Paťa", rele3ZaluzieAPort1.getRele3(), rele3ZaluzieAPort1.getRele4(), 35000, 1600),
                zaluzieMarek = new LouversControllerImpl("Marek", rele4ZaluzieAPort2.getRele1(), rele4ZaluzieAPort2.getRele2(), 35000, 1600),

                zaluzieLoznice1 = new LouversControllerImpl("Ložnice 1", rele4ZaluzieAPort2.getRele5(), rele4ZaluzieAPort2.getRele6(), 28000, 1600),
                zaluzieLoznice2 = new LouversControllerImpl("Ložnice 2", rele3ZaluzieAPort1.getRele5(), rele3ZaluzieAPort1.getRele6(), 28000, 1600),
                zaluzieSatna = new LouversControllerImpl("Šatna", rele8Actor3Port1.getRele3(), rele8Actor3Port1.getRele4(), 39000, 1600),
                zaluziePracovna = new LouversControllerImpl("Pracovna", rele7ZaluzieBPort3.getRele1(), rele7ZaluzieBPort3.getRele2(), 54000, 1600),

                zaluzieKuchyn = new LouversControllerImpl("Kuchyně", rele2ZaluzieAPort3.getRele5(), rele2ZaluzieAPort3.getRele6(), 58000, 1600),
                zaluzieObyvak1 = new LouversControllerImpl("Obývák 1", rele2ZaluzieAPort3.getRele1(), rele2ZaluzieAPort3.getRele2(), 58000, 1600),
                zaluzieObyvak2 = new LouversControllerImpl("Obývák 2", rele8Actor3Port1.getRele5(), rele8Actor3Port1.getRele6(), 58000, 1600),
                zaluzieObyvak3 = new LouversControllerImpl("Obývák 3", rele2ZaluzieAPort3.getRele3(), rele2ZaluzieAPort3.getRele4(), 58000, 1600),

                zaluzieObyvak4 = new LouversControllerImpl("Obývák 4", rele4ZaluzieAPort2.getRele3(), rele4ZaluzieAPort2.getRele4(), 58000, 1600),
                zaluzieObyvak5 = new LouversControllerImpl("Obývák 5", rele7ZaluzieBPort3.getRele3(), rele7ZaluzieBPort3.getRele4(), 34000, 1600),
                zaluzieObyvak6 = new LouversControllerImpl("Obývák 6", rele7ZaluzieBPort3.getRele5(), rele7ZaluzieBPort3.getRele6(), 20000, 1600),
                zaluzieKoupelnaDole = new LouversControllerImpl("Koupelna 2", rele8Actor3Port1.getRele1(), rele8Actor3Port1.getRele2(), 26000, 1600),

                zaluzieChodba1 = new LouversControllerImpl("Chodba 1", rele6ZaluzieBPort1.getRele3(), rele6ZaluzieBPort1.getRele4(), 39000, 1600),
                zaluzieChodba2 = new LouversControllerImpl("Chodba 2", rele6ZaluzieBPort1.getRele5(), rele6ZaluzieBPort1.getRele6(), 39000, 1600),
                zaluzieVratnice1 = new LouversControllerImpl("Vrátnice 1", rele5ZaluzieBPort2.getRele1(), rele5ZaluzieBPort2.getRele2(), 29000, 1600),
                zaluzieVratnice2 = new LouversControllerImpl("Vrátnice 2", rele5ZaluzieBPort2.getRele3(), rele5ZaluzieBPort2.getRele4(), 29000, 1600),
                zaluzieVratnice3 = new LouversControllerImpl("Vrátnice 3", rele5ZaluzieBPort2.getRele5(), rele5ZaluzieBPort2.getRele6(), 40000, 1600),
        };

        // lights
        // PWM
        LddBoardDevice lddDevice1 = new LddBoardDevice("lddDevice1", lddActorA, 1, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0);
        ArrayList<Action> lightsActions = new ArrayList<Action>();
        PwmActor marekPwmActor = addLddLight(lightsActions, "Marek", lddDevice1.getLdd1(), 0.95, new SwitchIndicator(marekSwA1.getRedLed(), true)); //.96
        PwmActor pataPwmActor = addLddLight(lightsActions, "Paťa", lddDevice1.getLdd2(), 0.95, new SwitchIndicator(patrikSw1.getRedLed(), true)); //.96
        PwmActor krystofPwmActor = addLddLight(lightsActions, "Kryštof", lddDevice1.getLdd3(), 0.95, new SwitchIndicator(krystofSwA2.getRedLed(), true)); //.96
        PwmActor koupelnaPwmActor = addLddLight(lightsActions, "Koupelna", lddDevice1.getLdd4(), 1.0, schodyDoleR3Sw.getGreenLedIndicator(false), koupelnaHoreSw1.getGreenLedIndicator(false), koupelnaHoreSw1.getRedLedIndicator(true)); // 1.08
        PwmActor loznice1PwmActor = addLddLight(lightsActions, "Ložnice velké", lddDevice1.getLdd5(), 1.0); //1.08
        PwmActor chodbaUPokojuPwmActor = addLddLight(lightsActions, "Chodba u pokoju", lddDevice1.getLdd6(), 1.0, new SwitchIndicator(chodbaHoreKoupelnaSw3.getRedLed(), true), new SwitchIndicator(chodbaHoreKrystofSwA3.getRedLed(), true), new SwitchIndicator(chodbaHorePatrikSw3.getRedLed(), true)); // 1.08

        LddBoardDevice lddDevice2 = new LddBoardDevice("lddDevice2", lddActorA, 2, 1.0, .7, .7, .7, .7, .35);
        PwmActor vratnice1PwmActor = addLddLight(lightsActions, "Vrátnice 1", lddDevice2.getLdd1(), 0.95, new SwitchIndicator(vratniceSw1.getRedLed(), true)); // .96
        PwmActor obyvak09PwmActor = addLddLight(lightsActions, "Obyvák 09 (x)", lddDevice2.getLdd2(), 0.7); // .72
        PwmActor obyvak08PwmActor = addLddLight(lightsActions, "Obyvák 08 (x)", lddDevice2.getLdd3(), 0.7); // .72
        PwmActor obyvak07PwmActor = addLddLight(lightsActions, "Obyvák 07 (x)", lddDevice2.getLdd4(), 0.7); // .72
        PwmActor obyvak03PwmActor = addLddLight(lightsActions, "Obyvák 03 (x)", lddDevice2.getLdd5(), 0.7); // .72
        PwmActor wcPwmActor = addLddLight(lightsActions, "WC", lddDevice2.getLdd6(), 0.24);

        LddBoardDevice lddDevice3 = new LddBoardDevice("lddDevice3", lddActorA, 3, .35, .35, .35, .35, .7, .7);
        PwmActor loznice2PwmActor = addLddLight(lightsActions, "Ložnice malé", lddDevice3.getLdd1(), 0.35, new SwitchIndicator(lozniceDvereSw2.getRedLed(), true), new SwitchIndicator(lozniceOknoSw2.getRedLed(), true)); // .36
        PwmActor satnaPwmActor = addLddLight(lightsActions, "Šatna", lddDevice3.getLdd2(), 0.35, new SwitchIndicator(chodbaALSw.getRedLed(), true), new SwitchIndicator(satnaSw3.getRedLed(), true)); //0.48
        PwmActor zadveriPwmActor = addLddLight(lightsActions, "Zádveří", lddDevice3.getLdd3(), 0.35, new SwitchIndicator(zadveriSwA1.getRedLed(), true)); // 0.48
        PwmActor koupelnaZrcadlaPwmActor = addLddLight(lightsActions, "Koupena zrcadla", lddDevice3.getLdd4(), 0.35); // .36
        PwmActor obyvak02PwmActor = addLddLight(lightsActions, "Obyvák 02 (x)", lddDevice3.getLdd5(), 0.7); // .72
        PwmActor chodbaSchodyPwmActor = addLddLight(lightsActions, "Chodba schody", lddDevice3.getLdd6(), 0.70); // .72

        LddBoardDevice lddDevice4 = new LddBoardDevice("lddDevice4", lddActorB, 1, .7, .7, .7, .7, .7, .7);
        PwmActor garaz1PwmActor = addLddLight(lightsActions, "Garáž 1", lddDevice4.getLdd1(), 0.7, new SwitchIndicator(zadveriSwA2.getRedLed(), false), new SwitchIndicator(garazASw1.getRedLed(), true)); // .72
        PwmActor garaz2PwmActor = addLddLight(lightsActions, "Garáž 2", lddDevice4.getLdd2(), 0.7); // .72
        PwmActor obyvak11PwmActor = addLddLight(lightsActions, "Obyvák 11 (x)", lddDevice4.getLdd3(), 0.7); // .72
        PwmActor kuchyn2PwmActor = addLddLight(lightsActions, "Kuchyň 2", lddDevice4.getLdd4(), 0.7, new SwitchIndicator(kuchynSw1.getRedLed(), true)); // .72
        PwmActor kuchyn3PwmActor = addLddLight(lightsActions, "Kuchyň 3", lddDevice4.getLdd5(), 0.7); // .72
        PwmActor kuchyn4PwmActor = addLddLight(lightsActions, "Kuchyň 4", lddDevice4.getLdd6(), 0.7); // .72

        LddBoardDevice lddDevice5 = new LddBoardDevice("lddDevice5", lddActorB, 2, .35, .35, 1.0, 1.0, 1.0, 1.0);
        PwmActor garaz3PwmActor = addLddLight(lightsActions, "Garáž 3", lddDevice5.getLdd1(), 0.35, new SwitchIndicator(zadveriSwA2.getGreenLed(), false)); // .36
        PwmActor koupelnaDoleZrcadlaPwmActor = addLddLight(lightsActions, "Koupelna dole zrcadla", lddDevice5.getLdd2(), 0.35); // .36
        PwmActor kuchyn1PwmActor = addLddLight(lightsActions, "Kuchyň 1", lddDevice5.getLdd3(), 0.7); // .72
        PwmActor obyvak05PwmActor = addLddLight(lightsActions, "Obyvák 05 (x)", lddDevice5.getLdd4(), 0.7); // .72
        PwmActor test55 = addLddLight(lightsActions, "test5.5", lddDevice5.getLdd5(), 0.1); // ?? (kuchyn stul?)
        PwmActor test56 = addLddLight(lightsActions, "test5.6", lddDevice5.getLdd6(), 0.1); // ?? (kuchyn stul?)

        LddBoardDevice lddDevice6 = new LddBoardDevice("lddDevice6", lddActorC, 2, .7, .7, .7, .7, .7, .7);
        PwmActor jidelna1PwmActor = addLddLight(lightsActions, "Jídelna 01", lddDevice6.getLdd1(), 0.7); // .72
        PwmActor obyvak06PwmActor = addLddLight(lightsActions, "Obyvák 06", lddDevice6.getLdd2(), 0.7); // .72
        PwmActor obyvak10PwmActor = addLddLight(lightsActions, "Obyvák 10", lddDevice6.getLdd3(), 0.7); // .72
        PwmActor obyvak01PwmActor = addLddLight(lightsActions, "Obyvák 01", lddDevice6.getLdd4(), 0.7); // .72
        PwmActor obyvak13PwmActor = addLddLight(lightsActions, "Obyvák 13", lddDevice6.getLdd5(), 0.7); // .72
        PwmActor pradelna1PwmActor = addLddLight(lightsActions, "Prádelna 1", lddDevice6.getLdd6(), 0.7, zadveriDoldePradelnaSw.getRedLedIndicator(false), pradelnaSw1.getRedLedIndicator(true)); // .72

        LddBoardDevice lddDevice7 = new LddBoardDevice("lddDevice7", lddActorC, 3, .7, .7, .7, .7, .7, .7);
        PwmActor pradelna2PwmActor = addLddLight(lightsActions, "Prádelna 2", lddDevice7.getLdd1(), 0.7); // .72
        PwmActor obyvak04PwmActor = addLddLight(lightsActions, "Obyvák 04 (x)", lddDevice7.getLdd2(), 0.7); // .72
        PwmActor jidelna2PwmActor = addLddLight(lightsActions, "Jidelna 2", lddDevice7.getLdd3(), 0.7); // .72
        PwmActor jidelna3PwmActor = addLddLight(lightsActions, "Jidelna 3", lddDevice7.getLdd4(), 0.7); // .72
        PwmActor kuchyn5PwmActor = addLddLight(lightsActions, "Kuchyň 5", lddDevice7.getLdd5(), 0.7); // .72
        PwmActor obyvak12PwmActor = addLddLight(lightsActions, "Obyvák 12 (x)", lddDevice7.getLdd6(), 0.05); // .72

        LddBoardDevice lddDevice8 = new LddBoardDevice("lddDevice8", lddActorC, 1, .6, .6, .5, .5, .5, .5);
        PwmActor pracovnaPwmActor = addLddLight(lightsActions, "Pracovna", lddDevice8.getLdd1(), 0.6); // .6
        PwmActor koupelnaDolePwmActor = addLddLight(lightsActions, "Koupelna dole", lddDevice8.getLdd2(), 0.6, new SwitchIndicator(koupelnaDoleSw2.getRedLed(), true), new SwitchIndicator(koupelnaDoleSw2.getGreenLed(), false)); // .60
        PwmActor vratnice2PwmActor = addLddLight(lightsActions, "Vrátnice 2", lddDevice8.getLdd3(), 0.48); //.48
        PwmActor chodbaDolePwmActor = addLddLight(lightsActions, "Chodba dole", lddDevice8.getLdd4(), 0.48); //.48
        PwmActor zadveriDolePwmActor = addLddLight(lightsActions, "Zádveří dole", lddDevice8.getLdd5(), 0.48, zadveriDoleChodbaSw.getGreenLedIndicator(false), zadveriDoleChodbaSw.getRedLedIndicator(true)); // .48
        PwmActor vchodHorePwmActor = addLddLight(lightsActions, "Vchod hore", lddDevice8.getLdd6(), 0.48); // .48


        // koupelna
        configurePwmLights(lst, koupelnaHoreSw1, WallSwitch.Side.LEFT, 50, koupelnaZrcadlaPwmActor);
        configurePwmLights(lst, koupelnaHoreSw1, WallSwitch.Side.RIGHT, 25, koupelnaPwmActor);
        lst.addActionBinding(new ActionBinding(schodyDoleR3Sw.getRightBottomButton(), new Action[]{new InvertAction(koupelnaPwmActor, 30)}, null));

        configureLouvers(lst, koupelnaHoreSw2, WallSwitch.Side.LEFT, zaluzieKoupelna);
        lst.addActionBinding(new ActionBinding(koupelnaHoreSw2.getRightUpperButton(), new Action[]{new SwitchOnSensorAction(zaricKoupelnaHore1Trubice, 900, 100), new SwitchOnSensorAction(zaricKoupelnaHore2Trubice, 900, 100)}, null));
        lst.addActionBinding(new ActionBinding(koupelnaHoreSw2.getRightBottomButton(), new Action[]{new SwitchOffAction(zaricKoupelnaHore1Trubice), new SwitchOffAction(zaricKoupelnaHore2Trubice)}, null));

        // koupelna u okna
        configureLouvers(lst, koupelnaHoreOknoSw, WallSwitch.Side.LEFT, zaluzieKoupelna);
        lst.addActionBinding(new ActionBinding(koupelnaHoreOknoSw.getRightUpperButton(), new Action[]{new SwitchOnSensorAction(zaricKoupelnaHore1Trubice, 900, 100), new SwitchOnSensorAction(zaricKoupelnaHore2Trubice, 900, 100)}, null));
        lst.addActionBinding(new ActionBinding(koupelnaHoreOknoSw.getRightBottomButton(), new Action[]{new SwitchOffAction(zaricKoupelnaHore1Trubice), new SwitchOffAction(zaricKoupelnaHore2Trubice)}, null));


        // kuchyn + obyvak
        configureLouvers(lst, schodyDoleR1Sw, WallSwitch.Side.LEFT, zaluzieKuchyn);
        configureLouvers(lst, schodyDoleR1Sw, WallSwitch.Side.RIGHT, zaluzieObyvak1);
        configureLouvers(lst, schodyDoleR2Sw, WallSwitch.Side.LEFT, zaluzieObyvak2, zaluzieObyvak3);
        configureLouvers(lst, schodyDoleR2Sw, WallSwitch.Side.RIGHT, zaluzieObyvak4);
        configureLouvers(lst, schodyDoleR3Sw, WallSwitch.Side.LEFT, zaluzieObyvak5, zaluzieObyvak6);

        // wc
        configurePwmLights(lst, wcSw, WallSwitch.Side.LEFT, 60, wcPwmActor);
        configurePwmLights(lst, wcSw, WallSwitch.Side.RIGHT, 60, wcPwmActor);


        // svetla satna
        configurePwmLights(lst, satnaSw3, WallSwitch.Side.RIGHT, 80, satnaPwmActor);
        configureLouvers(lst, satnaSw3, WallSwitch.Side.LEFT, zaluzieSatna);

        // zadveri
        configurePwmLights(lst, zadveriSwA1, WallSwitch.Side.LEFT, 80, zadveriPwmActor);

        SwitchOnSensorAction ovladacGarazAction = new SwitchOnSensorAction(ovladacGaraz, 1, 100);
        InvertActionWithTimer stomekAction = new InvertActionWithTimer(zasStromek, 12600);
        lst.addActionBinding(new ActionBinding(zadveriSwA1.getRightUpperButton(), new Action[]{ovladacGarazAction}, null));
        lst.addActionBinding(new ActionBinding(zadveriSwA1.getRightBottomButton(), new Action[]{stomekAction}, null));

        configurePwmLights(lst, zadveriSwA2, WallSwitch.Side.LEFT, 80, garaz1PwmActor, garaz2PwmActor);
        configurePwmLights(lst, zadveriSwA2, WallSwitch.Side.RIGHT, 100, garaz3PwmActor);
        configurePwmLights(lst, zadveriVratniceSw3, WallSwitch.Side.LEFT, 80, zadveriPwmActor);
        configurePwmLights(lst, zadveriVratniceSw3, WallSwitch.Side.RIGHT, 80, zadveriPwmActor);

        // garaz
        configurePwmLights(lst, garazASw1, WallSwitch.Side.LEFT, 80, garaz1PwmActor, garaz2PwmActor);
        configurePwmLights(lst, garazASw1, WallSwitch.Side.RIGHT, 100, garaz3PwmActor);
        lst.addActionBinding(new ActionBinding(garazASw2.getLeftUpperButton(), new Action[]{ovladacGarazAction}, null));
        lst.addActionBinding(new ActionBinding(garazASw2.getLeftBottomButton(), new Action[]{ovladacGarazAction}, null));

        // Krystof + Pata
        configureLouvers(lst, krystofSwA1, WallSwitch.Side.LEFT, zaluziePata);
        configureLouvers(lst, krystofSwA1, WallSwitch.Side.RIGHT, zaluzieKrystof);
        configurePwmLights(lst, krystofSwA2, WallSwitch.Side.LEFT, 50, pataPwmActor);
        configurePwmLights(lst, krystofSwA2, WallSwitch.Side.RIGHT, 50, krystofPwmActor);

        configurePwmLights(lst, krystofPostelSw, WallSwitch.Side.LEFT, 50, krystofPwmActor);
        configureLouvers(lst, krystofPostelSw, WallSwitch.Side.RIGHT, zaluzieKrystof, zaluziePata);

        configurePwmLights(lst, patrikSw1, WallSwitch.Side.LEFT, 50, pataPwmActor);
        configurePwmLights(lst, patrikSw1, WallSwitch.Side.RIGHT, 50, krystofPwmActor);

        configureLouvers(lst, patrikPostelSw3, WallSwitch.Side.LEFT, zaluziePata, zaluzieKrystof);
        configurePwmLights(lst, patrikPostelSw3, WallSwitch.Side.RIGHT, 50, pataPwmActor);

        // Marek
        configureLouvers(lst, marekSwA1, WallSwitch.Side.LEFT, zaluzieMarek);
        configurePwmLights(lst, marekSwA1, WallSwitch.Side.RIGHT, 50, marekPwmActor);
        configureLouvers(lst, marekPostelSw3, WallSwitch.Side.LEFT, zaluzieMarek);
        configurePwmLights(lst, marekPostelSw3, WallSwitch.Side.RIGHT, 50, marekPwmActor);


        // chodba hore - koupelna
        configurePwmLights(lst, chodbaHoreKoupelnaSw3, WallSwitch.Side.LEFT, 80, zadveriPwmActor);
        configurePwmLights(lst, chodbaHoreKoupelnaSw3, WallSwitch.Side.RIGHT, 40, chodbaSchodyPwmActor, chodbaUPokojuPwmActor);
        // switch off 4 lights
//        lst.addActionBinding(new ActionBinding(chodbaHoreKoupelnaSw3.getRightBottomButton(),
//                new Action[]{new SwitchOffAction(pataPwmActor), new SwitchOffAction(krystofPwmActor),
//                        new SwitchOffAction(marekPwmActor), new SwitchOffAction(satnaPwmActor)}, null));

        // chodba hore - krystof
        configurePwmLights(lst, chodbaHoreKrystofSwA3, WallSwitch.Side.LEFT, 40, chodbaUPokojuPwmActor, chodbaSchodyPwmActor);
        configurePwmLights(lst, chodbaHoreKrystofSwA3, WallSwitch.Side.RIGHT, 40, chodbaUPokojuPwmActor, chodbaSchodyPwmActor);

        // chodba hore - patrik
        configurePwmLights(lst, chodbaHorePatrikSw3, WallSwitch.Side.LEFT, 40, chodbaUPokojuPwmActor, chodbaSchodyPwmActor);
        configurePwmLights(lst, chodbaHorePatrikSw3, WallSwitch.Side.RIGHT, 80, satnaPwmActor);

        // chodba hore - u satny
        configurePwmLights(lst, chodbaALSw, WallSwitch.Side.LEFT, 80, satnaPwmActor);
        configurePwmLights(lst, chodbaALSw, WallSwitch.Side.RIGHT, 40, chodbaUPokojuPwmActor, chodbaSchodyPwmActor);
        configureLouvers(lst, chodbaARSw, WallSwitch.Side.LEFT, zaluzieSatna);
        configureLouvers(lst, chodbaARSw, WallSwitch.Side.RIGHT, zaluzieChodba2, zaluzieChodba1);

        // loznice
        configureLouvers(lst, lozniceOknoSw1, WallSwitch.Side.LEFT, zaluzieLoznice1);
        configureLouvers(lst, lozniceOknoSw1, WallSwitch.Side.RIGHT, zaluzieLoznice2);
        configureLouvers(lst, lozniceDvereSw1, WallSwitch.Side.LEFT, zaluzieLoznice1);
        configureLouvers(lst, lozniceDvereSw1, WallSwitch.Side.RIGHT, zaluzieLoznice2);
        configurePwmLights(lst, lozniceDvereSw2, WallSwitch.Side.LEFT, 40, loznice2PwmActor);
        configurePwmLights(lst, lozniceDvereSw2, WallSwitch.Side.RIGHT, 40, loznice1PwmActor);
        configurePwmLights(lst, lozniceOknoSw2, WallSwitch.Side.LEFT, 40, loznice1PwmActor);
        configurePwmLights(lst, lozniceOknoSw2, WallSwitch.Side.RIGHT, 40, loznice2PwmActor);

        //pracovna
        configureLouvers(lst, pracovnaSw2, WallSwitch.Side.LEFT, zaluziePracovna);
        configurePwmLights(lst, pracovnaSw2, WallSwitch.Side.RIGHT, 30, pracovnaPwmActor);

        // vratnice

        //TODO: Remove test 41
        WallSwitch test41Sw1 = new WallSwitch("Test41.1", switchTestNode41, 1);
        configureLouvers(lst, test41Sw1, WallSwitch.Side.RIGHT, zaluzieVratnice1);

        configureLouvers(lst, vratniceSw1, WallSwitch.Side.RIGHT, zaluzieVratnice1);
        configureLouvers(lst, vratniceSw2, WallSwitch.Side.LEFT, zaluzieVratnice2);
        configureLouvers(lst, vratniceSw2, WallSwitch.Side.RIGHT, zaluzieVratnice3);
        configurePwmLights(lst, vratniceSw1, WallSwitch.Side.LEFT, 40, vratnice1PwmActor, vratnice2PwmActor);

        // sklepy
        //    - zadveri
        lst.addActionBinding(new ActionBinding(zadveriDoleVchodRSw.getLeftUpperButton(), new Action[]{new SwitchOnSensorAction(svSklepLevy, 1800, 100)}, null));
        lst.addActionBinding(new ActionBinding(zadveriDoleVchodRSw.getLeftBottomButton(), new Action[]{new SwitchOffAction(svSklepLevy)}, null));

        lst.addActionBinding(new ActionBinding(zadveriDoleVchodRSw.getRightUpperButton(), new Action[]{new SwitchOnSensorAction(svSklepPravy, 1800, 100)}, null));
        lst.addActionBinding(new ActionBinding(zadveriDoleVchodRSw.getRightBottomButton(), new Action[]{new SwitchOffAction(svSklepPravy)}, null));

        //    - venku Levy
        lst.addActionBinding(new ActionBinding(sklepLevyLSw.getLeftUpperButton(), new Action[]{new SwitchOnSensorAction(svSklepLevy, 1800, 100)}, null));
        lst.addActionBinding(new ActionBinding(sklepLevyLSw.getLeftBottomButton(), new Action[]{new SwitchOffAction(svSklepLevy)}, null));
        lst.addActionBinding(new ActionBinding(sklepLevyLSw.getRightUpperButton(), new Action[]{new SwitchOnSensorAction(svSklepLevy, 1800, 100)}, null));
        lst.addActionBinding(new ActionBinding(sklepLevyLSw.getRightBottomButton(), new Action[]{new SwitchOffAction(svSklepLevy)}, null));

        //    - venku pravy
        lst.addActionBinding(new ActionBinding(sklepLevyRSw.getLeftUpperButton(), new Action[]{new SwitchOnSensorAction(svSklepPravy, 1800, 100)}, null));
        lst.addActionBinding(new ActionBinding(sklepLevyRSw.getLeftBottomButton(), new Action[]{new SwitchOffAction(svSklepPravy)}, null));
        lst.addActionBinding(new ActionBinding(sklepLevyRSw.getRightUpperButton(), new Action[]{new SwitchOnSensorAction(svSklepPravy, 1800, 100)}, null));
        lst.addActionBinding(new ActionBinding(sklepLevyRSw.getRightBottomButton(), new Action[]{new SwitchOffAction(svSklepPravy)}, null));


        // zadveri dole
        configurePwmLightsImpl(lst, zadveriDoldePradelnaSw, WallSwitch.Side.LEFT, 80, new PwmActor[]{pradelna1PwmActor}, new PwmActor[]{pradelna2PwmActor});
        configurePwmLightsImpl(lst, zadveriDoldePradelnaSw, WallSwitch.Side.RIGHT, 80, new PwmActor[]{pradelna1PwmActor}, new PwmActor[]{pradelna2PwmActor});

        configurePwmLights(lst, zadveriDoleChodbaSw, WallSwitch.Side.RIGHT, 40, zadveriDolePwmActor);
        configurePwmLights(lst, zadveriDoleChodbaSw, WallSwitch.Side.LEFT, 40, chodbaDolePwmActor);

        // chodba dole
        lst.addActionBinding(new ActionBinding(chodbaDoleSpajzSw3.getLeftUpperButton(), new Action[]{new SwitchOnSensorAction(svSpajza, 1800, 100, AbstractSensorAction.Priority.MEDIUM)}, null));
        lst.addActionBinding(new ActionBinding(chodbaDoleSpajzSw3.getLeftBottomButton(), new Action[]{new SwitchOffAction(svSpajza)}, null));
        configurePwmLights(lst, chodbaDoleSpajzSw3, WallSwitch.Side.RIGHT, 40, chodbaDolePwmActor);

        // pradelna
        configurePwmLights(lst, pradelnaSw1, WallSwitch.Side.LEFT, 60, pradelna1PwmActor);
        configurePwmLights(lst, pradelnaSw1, WallSwitch.Side.RIGHT, 60, pradelna2PwmActor);

//        lst.addActionBinding(new ActionBinding(pradelnaSw1.getLeftUpperButton(), new Action[]{new SwitchOnSensorAction(zasStromek, 1800, 100, AbstractSensorAction.Priority.MEDIUM)}, null));
//        lst.addActionBinding(new ActionBinding(pradelnaSw1.getLeftBottomButton(), new Action[]{new SwitchOffAction(zasStromek)}, null));
//        lst.addActionBinding(new ActionBinding(pradelnaSw1.getRightUpperButton(), new Action[]{new SwitchOnSensorAction(zasStromek, 1800, 100, AbstractSensorAction.Priority.MEDIUM)}, null));
//        lst.addActionBinding(new ActionBinding(pradelnaSw1.getRightBottomButton(), new Action[]{new SwitchOffAction(zasStromek)}, null));

        //koupelna dole
        configurePwmLights(lst, koupelnaDoleSw2, WallSwitch.Side.LEFT, 40, koupelnaDolePwmActor);
        configurePwmLights(lst, koupelnaDoleSw2, WallSwitch.Side.RIGHT, 80, koupelnaDoleZrcadlaPwmActor);
        configureLouvers(lst, koupelnaDoleSw1, WallSwitch.Side.RIGHT, zaluzieKoupelnaDole);

        // kuchyn
        NodePin bottomButton = getBottomButton(kuchynSw1, WallSwitch.Side.LEFT);
        lst.addActionBinding(new ActionBinding(bottomButton, new Action[]{
                new SwitchOffAction(chodbaDolePwmActor), new SwitchOffAction(zadveriDolePwmActor),
                new SwitchOffAction(pradelna1PwmActor), new SwitchOffAction(pradelna2PwmActor),
                new SwitchOffAction(svSpajza), new SwitchOffAction(koupelnaDolePwmActor), new SwitchOffAction(koupelnaDoleZrcadlaPwmActor)
        }, new Action[]{}));


        configurePwmLights(lst, kuchynSw1, WallSwitch.Side.RIGHT, 50, kuchyn1PwmActor, kuchyn2PwmActor, kuchyn3PwmActor);
        configurePwmLights(lst, kuchynSw2, WallSwitch.Side.RIGHT, 80, obyvak01PwmActor, obyvak10PwmActor, obyvak13PwmActor);
        configurePwmLights(lst, kuchynSw2, WallSwitch.Side.LEFT, 80, obyvak06PwmActor, jidelna1PwmActor);


        // PIRs
        InputDevice pirA1Prizemi = new InputDevice("pirA1Prizemi", pirNodeA, 1);
        setupPir(lst, pirA1Prizemi.getIn1AndActivate(), "Pradelna dvere", new SwitchOnSensorAction(pradelna1PwmActor, 600, 80), new SwitchOffSensorAction(pradelna1PwmActor, 60));
        setupPir(lst, pirA1Prizemi.getIn2AndActivate(), "Pradelna pracka", new SwitchOnSensorAction(pradelna1PwmActor, 600, 80), new SwitchOffSensorAction(pradelna1PwmActor, 60));
        setupPir(lst, pirA1Prizemi.getIn4AndActivate(), "Vchod hore", new SwitchOnSensorAction(vchodHorePwmActor, 600, 80, 0, -15), new SwitchOffSensorAction(vchodHorePwmActor, 60));
        setupPir(lst, pirA1Prizemi.getIn5AndActivate(), "Schodiste", null, null);

        InputDevice pirA2Patro = new InputDevice("pirA2Patro", pirNodeA, 2);
        setupPir(lst, pirA2Patro.getIn1AndActivate(), "Chodba pred WC", (Action) null, null);
        setupPir(lst, pirA2Patro.getIn2AndActivate(), "Chodba", (Action) null, null);
        setupPir(lst, pirA2Patro.getIn3AndActivate(), "WC", new SwitchOnSensorAction(wcPwmActor, 600, 100, 0, -15), new SwitchOffSensorAction(wcPwmActor, 60));
        setupPir(lst, pirA2Patro.getIn5AndActivate(), "Zadveri hore vchod", new SwitchOnSensorAction(zadveriPwmActor, 600, 100, 0, -15), new SwitchOffSensorAction(zadveriPwmActor, 15));
        setupPir(lst, pirA2Patro.getIn6AndActivate(), "Zadveri hore chodba", new SwitchOnSensorAction(zadveriPwmActor, 600, 100, 0, -15), new SwitchOffSensorAction(zadveriPwmActor, 15));
        setupPir(lst, pirA2Patro.getIn4AndActivate(), "Chodba nad Markem", new SwitchOnSensorAction(satnaPwmActor, 600, 66, 0, -15), new SwitchOffSensorAction(satnaPwmActor, 60));

        InputDevice pirA3Prizemi = new InputDevice("pirA3Prizemi", pirNodeA, 3);
        setupPir(lst, pirA3Prizemi.getIn1AndActivate(), "Jidelna", null, null);
        setupPir(lst, pirA3Prizemi.getIn2AndActivate(), "Obyvak", null, null);
        setupPir(lst, pirA3Prizemi.getIn3AndActivate(), "Chodba dole", new SwitchOnSensorAction(chodbaDolePwmActor, 600, 100, 0, -15), new SwitchOffSensorAction(chodbaDolePwmActor, 15));
        setupPir(lst, pirA3Prizemi.getIn4AndActivate(), "Koupelna dole", new SwitchOnSensorAction(koupelnaDolePwmActor, 600, 50, 0, -15), new SwitchOffSensorAction(koupelnaDolePwmActor, 60));
        setupPir(lst, pirA3Prizemi.getIn5AndActivate(), "Spajza", new SwitchOnSensorAction(svSpajza, 600, 100), new SwitchOffSensorAction(svSpajza, 20));
        setupPir(lst, pirA3Prizemi.getIn6AndActivate(), "Zadveri dole", new SwitchOnSensorAction(zadveriDolePwmActor, 600, 100, -15, -30), new SwitchOffSensorAction(zadveriDolePwmActor, 15));

        InputDevice cidlaGaraz = new InputDevice("cidlaGaraz", garazVzadu, 3);
        setupMagneticSensor(lst, cidlaGaraz.getIn1AndActivate(), "Garaz hore", null, null);
        setupMagneticSensor(lst, cidlaGaraz.getIn2AndActivate(), "Garaz dole", null, null);


        Servlet.action1 = new SwitchOnSensorAction(bzucakDvere, 3, 100);
        Servlet.action2 = ovladacGarazAction;
        Servlet.action3 = invertJidelna;

        Servlet.action4 = null;
        Servlet.action5 = null;
        Servlet.louversControllers = louversControllers;

        //test wall switch application
        WallSwitch testSw = new WallSwitch("testSwA", switchTestNode50, 1);
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
        setupSensor(lst, pirPin, name, activateAction, deactivateAction, true);
    }

    private static void setupMagneticSensor(SwitchListener lst, NodePin pirPin, String name, Action activateAction, Action deactivateAction) {
        setupSensor(lst, pirPin, name, activateAction, deactivateAction, false);
    }

    private static void setupSensor(SwitchListener lst, NodePin pirPin, String name, Action activateAction, Action deactivateAction, boolean logicalOneIsActivate) {
        PirStatus status = new PirStatus(name);
        Action[] activateActions = (activateAction != null) ? new Action[]{activateAction, status.getActivateAction()} : new Action[]{status.getActivateAction()};
        Action[] deactivateActions = (deactivateAction != null) ? new Action[]{deactivateAction, status.getDeactivateAction()} : new Action[]{status.getDeactivateAction()};
        if (logicalOneIsActivate) {
            lst.addActionBinding(new ActionBinding(pirPin, deactivateActions, activateActions));
        } else {
            lst.addActionBinding(new ActionBinding(pirPin, activateActions, deactivateActions));
        }
        pirStatusList.add(status);
    }

    private static void configurePwmLights(SwitchListener lst, WallSwitch wallSwitch, WallSwitch.Side side, int initialPwmValue, PwmActor... pwmActors) {
        configurePwmLightsImpl(lst, wallSwitch, side, initialPwmValue, pwmActors, null);
    }

    private static void configurePwmLightsImpl(SwitchListener lst, WallSwitch wallSwitch, WallSwitch.Side side, int initialPwmValue, PwmActor[] pwmActors, IOnOffActor[] switchOffOnlyActors) {
        List<Action> upperButtonUpActions = new ArrayList<>();
        List<Action> upperButtonDownActions = new ArrayList<>();
        List<Action> downButtonUpActions = new ArrayList<>();
        List<Action> downButtonDownActions = new ArrayList<>();

        for (PwmActor pwmActor : pwmActors) {
            PwmActionGroup actionGroup = new PwmActionGroup(pwmActor, initialPwmValue);
            upperButtonUpActions.add(actionGroup.getUpButtonDownAction());
            upperButtonDownActions.add(actionGroup.getUpButtonUpAction());
            downButtonUpActions.add(actionGroup.getDownButtonDownAction());
            downButtonDownActions.add(actionGroup.getDownButtonUpAction());
        }
        if (switchOffOnlyActors != null) {
            for (IOnOffActor pwmActor : switchOffOnlyActors) {
                downButtonDownActions.add(new SwitchOffAction(pwmActor));
            }
        }

        NodePin upperButton = getUpperButton(wallSwitch, side);
        NodePin bottomButton = getBottomButton(wallSwitch, side);
        lst.addActionBinding(new ActionBinding(upperButton, toArray(upperButtonUpActions), toArray(upperButtonDownActions)));
        lst.addActionBinding(new ActionBinding(bottomButton, toArray(downButtonUpActions), toArray(downButtonDownActions)));
    }

    private static Action[] toArray(List<Action> list) {
        return list.toArray(new Action[list.size()]);
    }

    static PwmActor addLddLight(ArrayList<Action> lightsActions, String name, LddBoardDevice.LddNodePin pin, double maxLoad, Indicator... indicators) {
        PwmActor pwmActor = new PwmActor(name, pin, maxLoad / pin.getMaxLddCurrent(), indicators);
        lightsActions.add(new SwitchOnAction(pwmActor));
        lightsActions.add(new IncreasePwmAction(pwmActor));
        lightsActions.add(new DecreasePwmAction(pwmActor));
        lightsActions.add(new SwitchOffAction(pwmActor));
        return pwmActor;
    }

    static void configureLouvers(SwitchListener lst, WallSwitch wallSwitch, WallSwitch.Side side, LouversController... louversControllers) {
        LouversActionGroup[] actionGroups = new LouversActionGroup[louversControllers.length];
        Action[] upButtonDownAction = new Action[louversControllers.length];
        Action[] upButtonUpAction = new Action[louversControllers.length];
        Action[] downButtonDownAction = new Action[louversControllers.length];
        Action[] downButtonUpAction = new Action[louversControllers.length];
        for (int i = 0; i < louversControllers.length; i++) {
            LouversActionGroup group = new LouversActionGroup(louversControllers[i]);
            upButtonDownAction[i] = group.getUpButtonDownAction();
            upButtonUpAction[i] = group.getUpButtonUpAction();
            downButtonDownAction[i] = group.getDownButtonDownAction();
            downButtonUpAction[i] = group.getDownButtonUpAction();
        }

        NodePin upTrigger = getUpperButton(wallSwitch, side);
        NodePin downTrigger = getBottomButton(wallSwitch, side);
        lst.addActionBinding(new ActionBinding(upTrigger, upButtonDownAction, upButtonUpAction));
        lst.addActionBinding(new ActionBinding(downTrigger, downButtonDownAction, downButtonUpAction));
    }

    private static NodePin getBottomButton(WallSwitch wallSwitch, WallSwitch.Side side) {
        return (side == WallSwitch.Side.LEFT) ? wallSwitch.getLeftBottomButton() : wallSwitch.getRightBottomButton();
    }

    private static NodePin getUpperButton(WallSwitch wallSwitch, WallSwitch.Side side) {
        return (side == WallSwitch.Side.LEFT) ? wallSwitch.getLeftUpperButton() : wallSwitch.getRightUpperButton();
    }
}
