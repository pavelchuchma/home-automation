package org.chuma.homecontroller.app.configurator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.chuma.homecontroller.app.servlet.pages.AbstractPage.VIRTUAL_CONFIGURATION_JS_FILENAME;

import org.chuma.homecontroller.app.servlet.Handler;
import org.chuma.homecontroller.app.servlet.Servlet;
import org.chuma.homecontroller.app.servlet.ServletAction;
import org.chuma.homecontroller.app.servlet.pages.GetBackendUrlJs;
import org.chuma.homecontroller.app.servlet.pages.LightsPage;
import org.chuma.homecontroller.app.servlet.pages.LouversPage;
import org.chuma.homecontroller.app.servlet.pages.NodeInfoDetailPage;
import org.chuma.homecontroller.app.servlet.pages.NodeInfoPage;
import org.chuma.homecontroller.app.servlet.pages.OptionsPage;
import org.chuma.homecontroller.app.servlet.pages.Page;
import org.chuma.homecontroller.app.servlet.pages.PirPage;
import org.chuma.homecontroller.app.servlet.pages.StaticPage;
import org.chuma.homecontroller.app.servlet.rest.AirValveHandler;
import org.chuma.homecontroller.app.servlet.rest.AllStatusHandler;
import org.chuma.homecontroller.app.servlet.rest.HvacHandler;
import org.chuma.homecontroller.app.servlet.rest.LouversHandler;
import org.chuma.homecontroller.app.servlet.rest.NodeHandler;
import org.chuma.homecontroller.app.servlet.rest.PirHandler;
import org.chuma.homecontroller.app.servlet.rest.PwmLightsHandler;
import org.chuma.homecontroller.app.servlet.rest.ServletActionHandler;
import org.chuma.homecontroller.app.servlet.rest.StatusHandler;
import org.chuma.homecontroller.app.servlet.rest.WaterPumpHandler;
import org.chuma.homecontroller.app.servlet.ws.WebSocketHandler;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.controller.ActionBinding;
import org.chuma.homecontroller.controller.action.AbstractSensorAction;
import org.chuma.homecontroller.controller.action.Action;
import org.chuma.homecontroller.controller.action.IndicatorAction;
import org.chuma.homecontroller.controller.action.InvertAction;
import org.chuma.homecontroller.controller.action.InvertActionWithTimer;
import org.chuma.homecontroller.controller.action.SwitchAllOffWithMemory;
import org.chuma.homecontroller.controller.action.SwitchOffAction;
import org.chuma.homecontroller.controller.action.SwitchOffSensorAction;
import org.chuma.homecontroller.controller.action.SwitchOnAction;
import org.chuma.homecontroller.controller.action.SwitchOnSensorAction;
import org.chuma.homecontroller.controller.action.condition.DarkCondition;
import org.chuma.homecontroller.controller.action.condition.ICondition;
import org.chuma.homecontroller.controller.action.condition.PressDurationCondition;
import org.chuma.homecontroller.controller.actor.ActorListener;
import org.chuma.homecontroller.controller.actor.IOnOffActor;
import org.chuma.homecontroller.controller.actor.OnOffActor;
import org.chuma.homecontroller.controller.actor.PwmActor;
import org.chuma.homecontroller.controller.actor.TestingOnOffActor;
import org.chuma.homecontroller.controller.controller.LouversController;
import org.chuma.homecontroller.controller.controller.LouversControllerImpl;
import org.chuma.homecontroller.controller.controller.ValveController;
import org.chuma.homecontroller.controller.controller.ValveControllerImpl;
import org.chuma.homecontroller.controller.device.InputDevice;
import org.chuma.homecontroller.controller.device.LddBoardDevice;
import org.chuma.homecontroller.controller.device.OutputDevice;
import org.chuma.homecontroller.controller.device.RelayBoardDevice;
import org.chuma.homecontroller.controller.device.SwitchIndicator;
import org.chuma.homecontroller.controller.device.WallSwitch;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoRegistry;
import org.chuma.homecontroller.controller.nodeinfo.NodeListener;
import org.chuma.homecontroller.extensions.action.condition.SunCondition;
import org.chuma.homecontroller.extensions.actor.HvacActor;
import org.chuma.homecontroller.extensions.actor.RadioOnOffActor;
import org.chuma.homecontroller.extensions.actor.WaterPumpMonitor;
import org.chuma.hvaccontroller.device.HvacDevice;

@SuppressWarnings({"unused", "DuplicatedCode", "SpellCheckingInspection"})
public class PiConfigurator extends AbstractConfigurator {

    static Logger log = LoggerFactory.getLogger(PiConfigurator.class.getName());

    public PiConfigurator(NodeInfoRegistry nodeInfoRegistry) {
        super(nodeInfoRegistry);
    }

    @Override
    public void configure() {
        OptionsSingleton.createInstance("cfg/app.properties", "default-pi.properties");

        Node bridge = nodeInfoRegistry.createNode(1, "Bridge");
        Node zadveri = nodeInfoRegistry.createNode(2, "Zadveri");
        Node actor3 = nodeInfoRegistry.createNode(3, "Actor3");
        Node zaluzieB = nodeInfoRegistry.createNode(4, "ZaluzieB");
        Node schodyDoleL = nodeInfoRegistry.createNode(5, "SchodyDoleL");
        Node lozniceOkno = nodeInfoRegistry.createNode(6, "LozniceOkno");
        Node pirNodeA = nodeInfoRegistry.createNode(7, "PirNodeA");
        Node zadveriDoleChodba = nodeInfoRegistry.createNode(8, "ZadveriDoleUChodby");
        Node koupelnaHore = nodeInfoRegistry.createNode(9, "KoupelnaHore");
        Node vratnice = nodeInfoRegistry.createNode(10, "Vratnice");
        Node schodyDoleR = nodeInfoRegistry.createNode(33, "SchodyDoleR");
        Node krystof = nodeInfoRegistry.createNode(12, "Krystof");
        Node zaluzieA = nodeInfoRegistry.createNode(13, "ZaluzieA");
        Node marek = nodeInfoRegistry.createNode(14, "Marek");
        Node patrik = nodeInfoRegistry.createNode(15, "Patrik");
        Node chodbaVzadu = nodeInfoRegistry.createNode(16, "ChodbaVzadu");
        Node zadveriDoleVchod = nodeInfoRegistry.createNode(17, "ZadveriDoleUVchodu");
        Node pracovna = nodeInfoRegistry.createNode(18, "Pracovna");
        Node lddActorA = nodeInfoRegistry.createNode(19, "LDD-ActorA");
        Node garazVzadu = nodeInfoRegistry.createNode(20, "GarazVzadu");
        Node lozniceDvere = nodeInfoRegistry.createNode(21, "LozniceDvere");
        Node garazVpredu = nodeInfoRegistry.createNode(22, "GarazVpredu");
        Node pradelna = nodeInfoRegistry.createNode(23, "Pradelna");
        Node lddActorB = nodeInfoRegistry.createNode(24, "LDD-ActorB");
        Node koupelnaDole = nodeInfoRegistry.createNode(25, "KoupelnaDole");
        Node kuchyn = nodeInfoRegistry.createNode(26, "KuchynDole");
        Node obyvakGauc = nodeInfoRegistry.createNode(27, "ObyvakGauc");
        Node obyvakVzaduR = nodeInfoRegistry.createNode(28, "ObyvakVzaduR");
        Node lddActorC = nodeInfoRegistry.createNode(29, "LDD-ActorC");
        Node obyvakVzaduL = nodeInfoRegistry.createNode(30, "ObyvakVzaduL");
        Node sklep = nodeInfoRegistry.createNode(31, "Sklep");
        Node actor4 = nodeInfoRegistry.createNode(32, "Actor4");
        Node rozvadecDole = nodeInfoRegistry.createNode(34, "RozvadecDole");
        Node lozniceZed = nodeInfoRegistry.createNode(35, "LozniceZed");
        Node chodbaOkno = nodeInfoRegistry.createNode(39, "ChodbaOkno");
        Node switchTestNode50 = nodeInfoRegistry.createNode(50, "SwitchTestNode50");
        Node switchTestNode41 = nodeInfoRegistry.createNode(41, "SwitchTestNode41");

        WallSwitch chodbaOkno1Sw = new WallSwitch("chodbaOkno1Sw", chodbaOkno, 1);
        WallSwitch chodbaOkno2Sw = new WallSwitch("chodbaOkno2Sw", chodbaOkno, 2);
        WallSwitch chodbaOkno3Sw = new WallSwitch("chodbaOkno3Sw", chodbaOkno, 3);

        WallSwitch chodbaVzaduLSw = new WallSwitch("chodbaVzaduLSw", chodbaVzadu, 1);
        WallSwitch chodbaVzaduRSw = new WallSwitch("chodbaVzaduRSw", chodbaVzadu, 2);
        WallSwitch wcSw = new WallSwitch("wcSw", chodbaVzadu, 3);

        WallSwitch schodyDoleL1Sw = new WallSwitch("schodyDoleL1Sw", schodyDoleL, 1);
        WallSwitch schodyDoleL2Sw = new WallSwitch("schodyDoleL2Sw", schodyDoleL, 2);
        WallSwitch schodyDoleR1Sw = new WallSwitch("schodyDoleR1Sw", schodyDoleR, 1);
        WallSwitch schodyDoleR2Sw = new WallSwitch("schodyDoleR2Sw", schodyDoleR, 2);
        WallSwitch schodyDoleR3Sw = new WallSwitch("schodyDoleR3Sw", schodyDoleR, 3);

        WallSwitch obyvakVzadu1Sw = new WallSwitch("ObyvakVzadu1Sw", obyvakVzaduL, 1);
        WallSwitch obyvakVzadu2Sw = new WallSwitch("ObyvakVzadu2Sw", obyvakVzaduL, 2);
        WallSwitch obyvakVzadu3Sw = new WallSwitch("ObyvakVzadu3Sw", obyvakVzaduR, 1);
        WallSwitch obyvakVzadu4Sw = new WallSwitch("ObyvakVzadu4Sw", obyvakVzaduR, 2);

        WallSwitch obyvakGaucLSw = new WallSwitch("ObyvakGaucLSw", obyvakGauc, 1);
        WallSwitch obyvakGaucRSw = new WallSwitch("ObyvakGaucRSw", obyvakGauc, 2);

        WallSwitch sklepLevyLSw = new WallSwitch("sklepLevyLSw", sklep, 1);
        WallSwitch sklepLevyRSw = new WallSwitch("sklepLevyRSw", sklep, 3);
        WallSwitch sklepPravySw = new WallSwitch("sklepPravySw", sklep, 2);

        OutputDevice triak1 = new OutputDevice("triak1", actor4, 3);
        RelayBoardDevice rele01 = new RelayBoardDevice("rele01", actor3, 3);
        RelayBoardDevice rele09 = new RelayBoardDevice("rele09", actor4, 2);
        RelayBoardDevice rele10 = new RelayBoardDevice("rele10", actor4, 1);
        RelayBoardDevice rele11 = new RelayBoardDevice("rele11", actor3, 1);

        WallSwitch zadveriSwA1 = new WallSwitch("zadveriSwA1", zadveri, 1);
        WallSwitch zadveriSwA2 = new WallSwitch("zadveriSwA2", zadveri, 2);
        WallSwitch zvonekPravySw = new WallSwitch("zvonekPravySw", zadveri, 3);
        WallSwitch zvonekLevySw = new WallSwitch("zvonekLevySw", garazVpredu, 3);

        WallSwitch koupelnaHoreSw1 = new WallSwitch("koupelnaHoreSw1", koupelnaHore, 1);
        WallSwitch koupelnaHoreSw2 = new WallSwitch("koupelnaHoreSw2", koupelnaHore, 2);
        WallSwitch chodbaHoreKoupelnaSw3 = new WallSwitch("chodbaHoreKoupelnaSw3", koupelnaHore, 3);
        WallSwitch zadveriDoleChodbaSw = new WallSwitch("zadveriDoleChodbaSw", zadveriDoleChodba, 1);
        WallSwitch zadveriDolePradelnaSw = new WallSwitch("zadveriDolePradelnaSw", zadveriDoleChodba, 3);
        WallSwitch lozniceOknoSw1 = new WallSwitch("lozniceOknoSw1", lozniceOkno, 1);
        WallSwitch lozniceOknoSw2 = new WallSwitch("lozniceOknoSw2", lozniceOkno, 2);
        WallSwitch lozniceDvereSw1 = new WallSwitch("lozniceDvereSw1", lozniceDvere, 1);
        WallSwitch lozniceDvereSw2 = new WallSwitch("lozniceDvereSw2", lozniceDvere, 2);
        WallSwitch marekPostelSw3 = new WallSwitch("marekPostelSw3", lozniceDvere, 3);
        WallSwitch lozniceZedSw1 = new WallSwitch("lozniceZedSw1", lozniceZed, 1);
        WallSwitch lozniceZedSw2 = new WallSwitch("lozniceZedSw2", lozniceZed, 2);
        WallSwitch lozniceZedLampySw = new WallSwitch("lozniceZedLampySw", lozniceZed, 3);
        WallSwitch vratniceSw1 = new WallSwitch("vratniceSw1", vratnice, 1);
        WallSwitch vratniceSw2 = new WallSwitch("vratniceSw2", vratnice, 2);
        WallSwitch zadveriVratniceSw3 = new WallSwitch("zadveriVratniceSw3", vratnice, 3);
        WallSwitch krystofSwA1 = new WallSwitch("krystofSwA1", krystof, 1);
        WallSwitch krystofSwA2 = new WallSwitch("krystofSwA2", krystof, 2);
        WallSwitch chodbaHoreKrystofSwA3 = new WallSwitch("chodbaHoreKrystofSwA3", krystof, 3);
        WallSwitch marekSwA1 = new WallSwitch("marekSwA1", marek, 1);
        WallSwitch marekSwA2 = new WallSwitch("marekSwA2", marek, 2);
        WallSwitch patrikPostelSw3 = new WallSwitch("patrikPostelSw3", marek, 3);
        WallSwitch krystofPostelSw = new WallSwitch("krystofPostel", zadveriDoleChodba, 2);
        WallSwitch patrikSw1 = new WallSwitch("pataSw1", patrik, 1);
        WallSwitch patrikSw2 = new WallSwitch("pataSw2", patrik, 2);
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
        WallSwitch kuchynLSw1 = new WallSwitch("kuchynLSw1", kuchyn, 3);
        WallSwitch kuchynLSw2 = new WallSwitch("kuchynLSw2", kuchyn, 2);
        WallSwitch kuchynLSw3 = new WallSwitch("kuchynLSw3", kuchyn, 1);
        WallSwitch kuchynRSw1 = new WallSwitch("kuchynRSw1", rozvadecDole, 1);

        HvacDevice hvacDevice = startHvacDevice();
        HvacActor hvacActor = new HvacActor(hvacDevice, "hvac", "HVAC");
        ICondition durationInfra = new PressDurationCondition(0, 1_000);
        ICondition durationHvac = new PressDurationCondition(2_500, 10_000);

        ActorListener prizemiVzaduKuchynSw2Indicator = kuchynLSw1.getGreenLedIndicator(SwitchIndicator.Mode.SIGNAL_ANY_ON);
        ActorListener schodyDoleJidelnaSw3Indicator = schodyDoleR3Sw.getRedLedIndicator(SwitchIndicator.Mode.SIGNAL_ALL_OFF);

        OnOffActor pisoarHore = new OnOffActor("pisoarHore", "PisoarHore", triak1.getOut1(), 1, 0);
        OnOffActor svSklepLevy = new OnOffActor("svSklepLevy", "Levy Sklep", triak1.getOut2(), 1, 0, prizemiVzaduKuchynSw2Indicator, sklepLevyRSw.getRedLedIndicator(SwitchIndicator.Mode.SIGNAL_ANY_ON), zadveriDoleVchodRSw.getGreenLedIndicator(SwitchIndicator.Mode.SIGNAL_ANY_ON));
        OnOffActor pisoarDole = new OnOffActor("pisoarDole", "Pisoar dole", triak1.getOut4(), 1, 0);
        OnOffActor svSklepPravy = new OnOffActor("svSklepPravy", "Pravy Sklep", triak1.getOut5(), 1, 0, prizemiVzaduKuchynSw2Indicator, sklepPravySw.getRedLedIndicator(SwitchIndicator.Mode.SIGNAL_ANY_ON), zadveriDoleVchodRSw.getRedLedIndicator(SwitchIndicator.Mode.SIGNAL_ANY_ON));
        OnOffActor zasStromek = new OnOffActor("zasStromek", "Zasuvka Stromek", triak1.getOut6(), 1, 0, schodyDoleL1Sw.getGreenLedIndicator(SwitchIndicator.Mode.SIGNAL_ANY_ON));

        SwitchIndicator zaricKoupelnaHoreSw2Indicator = new SwitchIndicator(koupelnaHoreSw2.getRedLed(), SwitchIndicator.Mode.SIGNAL_ANY_ON);
        SwitchIndicator zaricKoupelnaHoreOknoSwIndicator = new SwitchIndicator(koupelnaHoreOknoSw.getRedLed(), SwitchIndicator.Mode.SIGNAL_ANY_ON);
        OnOffActor zaricKoupelnaHore2Trubice = new OnOffActor("zaricKoupelnaHore2Trubice", "Zaric koupelna 2 rubice", rele01.getRelay1(), 0, 1, zaricKoupelnaHoreSw2Indicator, zaricKoupelnaHoreOknoSwIndicator);
        OnOffActor zaricKoupelnaHore1Trubice = new OnOffActor("zaricKoupelnaHore1Trubice", "Zaric koupelna 1 rubice", rele01.getRelay2(), 0, 1, zaricKoupelnaHoreSw2Indicator, zaricKoupelnaHoreOknoSwIndicator);
        OnOffActor obyvakZasLZvonek = new OnOffActor("obyvakZasL", "ObyvakZasLZvonek", rele01.getRelay3(), 0, 1, zvonekPravySw.getRedLedIndicator(SwitchIndicator.Mode.SIGNAL_ALL_OFF), zvonekLevySw.getGreenLedIndicator(SwitchIndicator.Mode.SIGNAL_ALL_OFF));

        RelayBoardDevice rele12 = new RelayBoardDevice("rele12", rozvadecDole, 2);
        OnOffActor ovladacGaraz = new OnOffActor("ovladacGaraz", "Vrata garaz", rele12.getRelay2(), 0, 1);
        OnOffActor bzucakDvere = new OnOffActor("bzucakDvere", "Bzucak Dvere", rele12.getRelay1(), 0, 1);
        OnOffActor malyStromek = new OnOffActor("malyStromek", "Stromek pred dvermi", rele12.getRelay3(), 0, 1);

        NodeListener lst = nodeInfoRegistry.getNodeListener();

        // zaluzie
        RelayBoardDevice rele3ZaluzieAPort1 = new RelayBoardDevice("rele3ZaluzieAPort1", zaluzieA, 1);
        RelayBoardDevice rele4ZaluzieAPort2 = new RelayBoardDevice("rele3ZaluzieAPort1", zaluzieA, 2);
        RelayBoardDevice rele2ZaluzieAPort3 = new RelayBoardDevice("rele3ZaluzieAPort1", zaluzieA, 3);

        RelayBoardDevice rele6ZaluzieBPort1 = new RelayBoardDevice("rele6ZaluzieBPort1", zaluzieB, 1);
        RelayBoardDevice rele5ZaluzieBPort2 = new RelayBoardDevice("rele5ZaluzieBPort2", zaluzieB, 2);
        RelayBoardDevice rele7ZaluzieBPort3 = new RelayBoardDevice("rele7ZaluzieBPort3", zaluzieB, 3);

        RelayBoardDevice rele8Actor3Port1 = new RelayBoardDevice("rele8Actor3Port1", actor3, 2);

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

//        int snowConstant = 3000;
        int snowConstant = 0;
        LouversController[] louversControllers = new LouversController[]{
                zaluzieKoupelna = new LouversControllerImpl("lvKoupH", "Koupelna", rele6ZaluzieBPort1.getRelay1(), rele6ZaluzieBPort1.getRelay2(), 39000, 1600),
                zaluzieKrystof = new LouversControllerImpl("lvKrys", "Kry??tof", rele3ZaluzieAPort1.getRelay1(), rele3ZaluzieAPort1.getRelay2(), 35000, 1600),
                zaluziePata = new LouversControllerImpl("lvPata", "Pa??a", rele3ZaluzieAPort1.getRelay3(), rele3ZaluzieAPort1.getRelay4(), 35000, 1600),
                zaluzieMarek = new LouversControllerImpl("lvMarek", "Marek", rele4ZaluzieAPort2.getRelay1(), rele4ZaluzieAPort2.getRelay2(), 35000, 1600),

                zaluzieLoznice1 = new LouversControllerImpl("lvLoz1", "Lo??nice 1", rele4ZaluzieAPort2.getRelay5(), rele4ZaluzieAPort2.getRelay6(), 28000, 1600),
                zaluzieLoznice2 = new LouversControllerImpl("lvLoz2", "Lo??nice 2", rele3ZaluzieAPort1.getRelay5(), rele3ZaluzieAPort1.getRelay6(), 28000, 1600),
                zaluzieSatna = new LouversControllerImpl("lvSat", "??atna", rele8Actor3Port1.getRelay3(), rele8Actor3Port1.getRelay4(), 39000, 1600),
                zaluziePracovna = new LouversControllerImpl("lvPrc", "Pracovna", rele7ZaluzieBPort3.getRelay1(), rele7ZaluzieBPort3.getRelay2(), 54000, 1600),

                zaluzieKuchyn = new LouversControllerImpl("lvKuch", "Kuchy??", rele2ZaluzieAPort3.getRelay5(), rele2ZaluzieAPort3.getRelay6(), 58000 - snowConstant, 1600),
                zaluzieObyvak1 = new LouversControllerImpl("lvOb1", "Ob??v??k 1", rele2ZaluzieAPort3.getRelay1(), rele2ZaluzieAPort3.getRelay2(), 57000 - snowConstant, 1600),
                zaluzieObyvak2 = new LouversControllerImpl("lvOb2", "Ob??v??k 2", rele8Actor3Port1.getRelay5(), rele8Actor3Port1.getRelay6(), 53000, 1600),
                zaluzieObyvak3 = new LouversControllerImpl("lvOb3", "Ob??v??k 3", rele2ZaluzieAPort3.getRelay3(), rele2ZaluzieAPort3.getRelay4(), 58000 - snowConstant, 1600),

                zaluzieObyvak4 = new LouversControllerImpl("lvOb4", "Ob??v??k 4", rele4ZaluzieAPort2.getRelay3(), rele4ZaluzieAPort2.getRelay4(), 58000 - snowConstant, 1600),
                zaluzieObyvak5 = new LouversControllerImpl("lvOb5", "Ob??v??k 5", rele7ZaluzieBPort3.getRelay3(), rele7ZaluzieBPort3.getRelay4(), 34000, 1600),
                zaluzieObyvak6 = new LouversControllerImpl("lvOb6", "Ob??v??k 6", rele7ZaluzieBPort3.getRelay5(), rele7ZaluzieBPort3.getRelay6(), 20000, 1600),
                zaluzieKoupelnaDole = new LouversControllerImpl("lvKoupD", "Koupelna dole", rele8Actor3Port1.getRelay1(), rele8Actor3Port1.getRelay2(), 26000, 1600),

                zaluzieChodba1 = new LouversControllerImpl("lvCh1", "Chodba 1", rele6ZaluzieBPort1.getRelay3(), rele6ZaluzieBPort1.getRelay4(), 39000, 1600),
                zaluzieChodba2 = new LouversControllerImpl("lvCh2", "Chodba 2", rele6ZaluzieBPort1.getRelay5(), rele6ZaluzieBPort1.getRelay6(), 39000, 1600),
                zaluzieVratnice1 = new LouversControllerImpl("lvVrt1", "Vr??tnice 1", rele5ZaluzieBPort2.getRelay1(), rele5ZaluzieBPort2.getRelay2(), 29000, 1600),
                zaluzieVratnice2 = new LouversControllerImpl("lvVrt2", "Vr??tnice 2", rele5ZaluzieBPort2.getRelay3(), rele5ZaluzieBPort2.getRelay4(), 29000, 1600),
                zaluzieVratnice3 = new LouversControllerImpl("lvVrt3", "Vr??tnice 3", rele5ZaluzieBPort2.getRelay5(), rele5ZaluzieBPort2.getRelay6(), 40000, 1600),
        };

        ValveController[] valveControllers = new ValveController[]{
                new ValveControllerImpl("vlVrt", "Vratnice", rele11.getRelay1(), rele11.getRelay2(), 150000),
                new ValveControllerImpl("vlPrc", "Pracovna", rele11.getRelay3(), rele11.getRelay4(), 150000),
                new ValveControllerImpl("vlKoupD", "KoupelnaDole", rele11.getRelay5(), rele11.getRelay6(), 150000),

                new ValveControllerImpl("vlJid", "Jidelna", rele09.getRelay1(), rele09.getRelay2(), 150000),
                new ValveControllerImpl("vlKoupH", "KoupelnaHore", rele09.getRelay3(), rele09.getRelay4(), 150000),
                new ValveControllerImpl("vlPata", "Pata", rele09.getRelay5(), rele09.getRelay6(), 150000),

                new ValveControllerImpl("vlObyv23", "Obyvak 2+3", rele10.getRelay1(), rele10.getRelay2(), 150000),
                new ValveControllerImpl("vlMarek", "Marek", rele10.getRelay3(), rele10.getRelay4(), 150000),
                new ValveControllerImpl("vlObyv45", "Obyvak 4+5", rele10.getRelay5(), rele10.getRelay6(), 150000),
        };

        SwitchIndicator krystofIndicator = new SwitchIndicator(krystofSwA2.getRedLed(), SwitchIndicator.Mode.SIGNAL_ALL_OFF);
        SwitchIndicator pataIndicator = new SwitchIndicator(patrikSw2.getRedLed(), SwitchIndicator.Mode.SIGNAL_ALL_OFF);

        SwitchIndicator pradelnaOnIndicator = new SwitchIndicator(zadveriDolePradelnaSw.getRedLed(), SwitchIndicator.Mode.SIGNAL_ANY_ON); // red & green is swapped on this switch
        SwitchIndicator pradelnaOffIndicator = new SwitchIndicator(pradelnaSw1.getRedLed(), SwitchIndicator.Mode.SIGNAL_ALL_OFF);

        // lights
        // PWM
        ArrayList<PwmActor> pwmActors = new ArrayList<>();
        SwitchIndicator lozniceDvereSw2Indicator = new SwitchIndicator(lozniceDvereSw2.getRedLed(), SwitchIndicator.Mode.SIGNAL_ALL_OFF);
        SwitchIndicator lozniceOknoSw2Indicator = new SwitchIndicator(lozniceOknoSw2.getRedLed(), SwitchIndicator.Mode.SIGNAL_ALL_OFF);
        SwitchIndicator lozniceZedSw2Indicator = new SwitchIndicator(lozniceZedSw2.getRedLed(), SwitchIndicator.Mode.SIGNAL_ALL_OFF);
        LddBoardDevice lddDevice1 = new LddBoardDevice("lddDevice1", lddActorA, 1, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0);
        PwmActor marekPwmActor = addLddLight(pwmActors, "pwmMarek", "Marek", lddDevice1.getLdd1(), 0.95, marekSwA2.getRedLedIndicator(SwitchIndicator.Mode.SIGNAL_ALL_OFF), marekSwA2.getGreenLedIndicator(SwitchIndicator.Mode.SIGNAL_ANY_ON)); //.96
        PwmActor pataPwmActor = addLddLight(pwmActors, "pwmPata", "Pa??a", lddDevice1.getLdd2(), 0.95, krystofIndicator, pataIndicator); //.96
        PwmActor krystofPwmActor = addLddLight(pwmActors, "pwmKry", "Kry??tof", lddDevice1.getLdd3(), 0.95, krystofIndicator, pataIndicator); //.96
        PwmActor koupelnaPwmActor = addLddLight(pwmActors, "pwmKpH", "Koupelna", lddDevice1.getLdd4(), 1.0, koupelnaHoreSw1.getGreenLedIndicator(SwitchIndicator.Mode.SIGNAL_ANY_ON), koupelnaHoreSw1.getRedLedIndicator(SwitchIndicator.Mode.SIGNAL_ALL_OFF)); // 1.08
        PwmActor loznice1PwmActor = addLddLight(pwmActors, "pwmLozV", "Lo??nice velk??", lddDevice1.getLdd5(), 1.0, lozniceDvereSw2Indicator, lozniceOknoSw2Indicator, lozniceZedSw2Indicator); //1.08
        PwmActor chodbaUPokojuPwmActor = addLddLight(pwmActors, "pwmChP", "Chodba u pokoju", lddDevice1.getLdd6(), 1.0, new SwitchIndicator(chodbaHoreKoupelnaSw3.getRedLed(), SwitchIndicator.Mode.SIGNAL_ALL_OFF), new SwitchIndicator(chodbaHoreKrystofSwA3.getRedLed(), SwitchIndicator.Mode.SIGNAL_ALL_OFF), new SwitchIndicator(chodbaHorePatrikSw3.getRedLed(), SwitchIndicator.Mode.SIGNAL_ALL_OFF)); // 1.08

        LddBoardDevice lddDevice2 = new LddBoardDevice("lddDevice2", lddActorA, 2, 1.0, .7, .7, .7, .7, .35);
//        PwmActor pradelna1PwmActor = addLddLight(lightsActions, "pwmPrd1", "Pr??delna 1", lddDevice2.getLdd1(), 1, prizemiVzaduKuchynSw2Indicator, pradelnaOnIndicator, pradelnaOffIndicator); // 1.05
        PwmActor pradelna1PwmActor = addLddLight(pwmActors, "pwmPrd1", "Pr??delna 1", lddDevice2.getLdd2(), 0.7, prizemiVzaduKuchynSw2Indicator, pradelnaOnIndicator, pradelnaOffIndicator); //  1.05
//        PwmActor obyvak09PwmActor = addLddLight(lightsActions, "pwmOb9", "Obyv??k 09", lddDevice2.getLdd2(), 0.7); // .72
        PwmActor obyvak08PwmActor = addLddLight(pwmActors, "pwmOb8", "Obyv??k 08", lddDevice2.getLdd3(), 0.7); // .72
        PwmActor satnaPwmActor = addLddLight(pwmActors, "pwmSat", "??atna", lddDevice2.getLdd4(), 0.48, new SwitchIndicator(chodbaVzaduLSw.getRedLed(), SwitchIndicator.Mode.SIGNAL_ALL_OFF), new SwitchIndicator(satnaSw3.getRedLed(), SwitchIndicator.Mode.SIGNAL_ALL_OFF)); //0.48
        PwmActor obyvak03PwmActor = addLddLight(pwmActors, "pwmOb3", "Obyv??k 03", lddDevice2.getLdd5(), 0.7); // .72
        PwmActor wcPwmActor = addLddLight(pwmActors, "pwmWc", "WC", lddDevice2.getLdd6(), 0.24, new SwitchIndicator(wcSw.getRedLed(), SwitchIndicator.Mode.SIGNAL_ALL_OFF));

        LddBoardDevice lddDevice3 = new LddBoardDevice("lddDevice3", lddActorA, 3, .7, .35, .35, .35, .7, .7);
        PwmActor obyvak07PwmActor = addLddLight(pwmActors, "pwmOb7", "Obyv??k 07", lddDevice3.getLdd1(), 0.7); // .72
        PwmActor pradelna3PwmActor = addLddLight(pwmActors, "pwmPrd3", "Pr??delna 3", lddDevice3.getLdd2(), 0.35, prizemiVzaduKuchynSw2Indicator, pradelnaOnIndicator, pradelnaOffIndicator); // .36
        PwmActor schodyPwmActor = addLddLight(pwmActors, "pwmSchd", "Schody", lddDevice3.getLdd3(), 0.35); // .6
        PwmActor koupelnaZrcadlaPwmActor = addLddLight(pwmActors, "pwmKpHZrc", "Koupena zrcadla", lddDevice3.getLdd4(), 0.35); // .36
        PwmActor obyvak02PwmActor = addLddLight(pwmActors, "pwmOb2", "Obyv??k 02", lddDevice3.getLdd5(), 0.7); // .72
        PwmActor chodbaSchodyPwmActor = addLddLight(pwmActors, "pwmChSch", "Chodba nad schody", lddDevice3.getLdd6(), 0.7); // .72


        LddBoardDevice lddDevice4 = new LddBoardDevice("lddDevice4", lddActorB, 1, .7, .7, .7, .7, .7, .7);
        SwitchIndicator garazZadveriSwAIndicator = new SwitchIndicator(zadveriSwA2.getRedLed(), SwitchIndicator.Mode.SIGNAL_ANY_ON);
        SwitchIndicator garazGarazSwAIndicator = new SwitchIndicator(garazASw1.getRedLed(), SwitchIndicator.Mode.SIGNAL_ALL_OFF);
        PwmActor obyvak05PwmActor = addLddLight(pwmActors, "pwmOb5", "Obyv??k 05", lddDevice4.getLdd1(), 0.7); // .72
        PwmActor kuchyn4PwmActor = addLddLight(pwmActors, "pwmKch4", "Kuchy?? 4", lddDevice4.getLdd2(), 0.7); // .72
        PwmActor obyvak11PwmActor = addLddLight(pwmActors, "pwmOb11", "Obyv??k 11", lddDevice4.getLdd3(), 0.7); // .72
        PwmActor kuchyn2PwmActor = addLddLight(pwmActors, "pwmKch2", "Kuchy?? 2", lddDevice4.getLdd4(), 0.7, new SwitchIndicator(kuchynLSw3.getRedLed(), SwitchIndicator.Mode.SIGNAL_ALL_OFF)); // .72
        PwmActor kuchyn3PwmActor = addLddLight(pwmActors, "pwmKch3", "Kuchy?? 3", lddDevice4.getLdd5(), 0.7); // .72
        PwmActor kuchyn1PwmActor = addLddLight(pwmActors, "pwmKch1", "Kuchy?? 1", lddDevice4.getLdd6(), 0.7); // .72

        ActorListener drevnikSwIndicator = sklepLevyLSw.getGreenLedIndicator(SwitchIndicator.Mode.SIGNAL_ANY_ON);
        LddBoardDevice lddDevice5 = new LddBoardDevice("lddDevice5", lddActorB, 2, .35, .35, 1.0, 1.0, 1.0, 1.0);
        PwmActor loznice2PwmActor = addLddLight(pwmActors, "pwmLozM", "Lo??nice mal??", lddDevice5.getLdd1(), 0.35, lozniceDvereSw2Indicator, lozniceOknoSw2Indicator, lozniceZedSw2Indicator); // .36
        PwmActor koupelnaDoleZrcadlaPwmActor = addLddLight(pwmActors, "pwmKpDZrc", "Koupelna dole zrcadla", lddDevice5.getLdd2(), 0.35, prizemiVzaduKuchynSw2Indicator); // .36
        PwmActor pudaPwmActor = addLddLight(pwmActors, "pwmPuda", "P??da", lddDevice5.getLdd3(), 0.96, garazASw2.getGreenLedIndicator(SwitchIndicator.Mode.SIGNAL_ANY_ON)); // .96
        PwmActor kuchynLinkaPwmActor = addLddLight(pwmActors, "pwmKuLi", "Kuchy??sk?? linka", lddDevice5.getLdd4(), 1.0); // .72
        PwmActor vratnice1PwmActor = addLddLight(pwmActors, "pwmVrt1", "Vr??tnice 1", lddDevice5.getLdd5(), 0.95, new SwitchIndicator(vratniceSw1.getRedLed(), SwitchIndicator.Mode.SIGNAL_ALL_OFF)); // .96
        PwmActor terasaPwmActor = addLddLight(pwmActors, "pwmTrs", "Terasa", lddDevice5.getLdd6(), 1.0, prizemiVzaduKuchynSw2Indicator); // 1.08

        LddBoardDevice lddDevice6 = new LddBoardDevice("lddDevice6", lddActorC, 2, .7, .7, .7, .7, .7, .7);
        PwmActor jidelna1PwmActor = addLddLight(pwmActors, "pwmJid1", "J??delna 1", lddDevice6.getLdd1(), 0.7); // .72
        PwmActor obyvak06PwmActor = addLddLight(pwmActors, "pwmOb6", "Obyv??k 06", lddDevice6.getLdd2(), 0.7); // .72
        PwmActor obyvak10PwmActor = addLddLight(pwmActors, "pwmOb10", "Obyv??k 10", lddDevice6.getLdd3(), 0.7); // .72
        PwmActor obyvak01PwmActor = addLddLight(pwmActors, "pwmOb1", "Obyv??k 01", lddDevice6.getLdd4(), 0.7); // .72
        PwmActor obyvak13PwmActor = addLddLight(pwmActors, "pwmOb13", "Obyv??k 13", lddDevice6.getLdd5(), 0.7); // .72
        PwmActor zadveriPwmActor = addLddLight(pwmActors, "pwmZadH", "Z??dve????", lddDevice6.getLdd6(), 0.48, new SwitchIndicator(zadveriSwA1.getRedLed(), SwitchIndicator.Mode.SIGNAL_ALL_OFF)); // 0.48

        LddBoardDevice lddDevice7 = new LddBoardDevice("lddDevice7", lddActorC, 3, .7, .7, .7, .7, .7, .7);
        PwmActor pradelna2PwmActor = addLddLight(pwmActors, "pwmPrd2", "Pr??delna 2", lddDevice7.getLdd1(), 0.7, prizemiVzaduKuchynSw2Indicator, pradelnaOnIndicator, pradelnaOffIndicator); // .72
        PwmActor obyvak04PwmActor = addLddLight(pwmActors, "pwmOb4", "Obyv??k 04", lddDevice7.getLdd2(), 0.7); // .72
        PwmActor jidelna2PwmActor = addLddLight(pwmActors, "pwmJid2", "J??delna 2", lddDevice7.getLdd3(), 0.7); // .72
        PwmActor jidelna3PwmActor = addLddLight(pwmActors, "pwmJid3", "J??delna 3", lddDevice7.getLdd4(), 0.7); // .72
        PwmActor kuchyn5PwmActor = addLddLight(pwmActors, "pwmKch5", "Kuchy?? 5", lddDevice7.getLdd5(), 0.7); // .72
        PwmActor obyvak12PwmActor = addLddLight(pwmActors, "pwmOb12", "Obyv??k 12", lddDevice7.getLdd6(), 0.7); // .72

        LddBoardDevice lddDevice8 = new LddBoardDevice("lddDevice8", lddActorC, 1, .6, .6, .5, .5, .5, .5);
        PwmActor pracovnaPwmActor = addLddLight(pwmActors, "pwmPrac", "Pracovna", lddDevice8.getLdd1(), 0.6); // .6
        PwmActor koupelnaDolePwmActor = addLddLight(pwmActors, "pwmKpD", "Koupelna dole", lddDevice8.getLdd2(), 0.6, prizemiVzaduKuchynSw2Indicator, new SwitchIndicator(koupelnaDoleSw2.getRedLed(), SwitchIndicator.Mode.SIGNAL_ALL_OFF), koupelnaDoleSw2.getGreenLedIndicator(SwitchIndicator.Mode.SIGNAL_ANY_ON)); // .60
        PwmActor vratnice2PwmActor = addLddLight(pwmActors, "pwmVrt2", "Vr??tnice 2", lddDevice8.getLdd3(), 0.48); //.48
        PwmActor chodbaDolePwmActor = addLddLight(pwmActors, "pwmChoD", "Chodba dole", lddDevice8.getLdd4(), 0.48, prizemiVzaduKuchynSw2Indicator); //.48
        PwmActor zadveriDolePwmActor = addLddLight(pwmActors, "pwmZadD", "Z??dve???? dole", lddDevice8.getLdd5(), 0.48, prizemiVzaduKuchynSw2Indicator, zadveriDoleChodbaSw.getGreenLedIndicator(SwitchIndicator.Mode.SIGNAL_ANY_ON), zadveriDoleChodbaSw.getRedLedIndicator(SwitchIndicator.Mode.SIGNAL_ALL_OFF)); // .48
        PwmActor vchodHorePwmActor = addLddLight(pwmActors, "pwmVchH", "Vchod hore", lddDevice8.getLdd6(), 0.48); // .48

        LddBoardDevice lddDevice9 = new LddBoardDevice("lddDevice9", lddActorB, 3, 1.5, 1.5, 1.5, 1, .7, .7);
        PwmActor jidelnaStulPwmActor = addLddLight(pwmActors, "pwmJdl", "Jidelna st??l", lddDevice9.getLdd1(), 1.5, schodyDoleJidelnaSw3Indicator); // 1.5
        PwmActor garaz1PwmActor = addLddLight(pwmActors, "pwmG1", "Gar???? 1", lddDevice9.getLdd2(), 1.44, garazZadveriSwAIndicator, garazGarazSwAIndicator); // 2x.72
        PwmActor garaz2PwmActor = addLddLight(pwmActors, "pwmG2", "Gar???? 2", lddDevice9.getLdd3(), 1.44, garazZadveriSwAIndicator, garazGarazSwAIndicator); // 2x.72
        PwmActor spajzPwmActor = addLddLight(pwmActors, "pwmSpjz", "??pajz", lddDevice9.getLdd4(), 1, prizemiVzaduKuchynSw2Indicator, chodbaDoleSpajzSw3.getRedLedIndicator(SwitchIndicator.Mode.SIGNAL_ANY_ON)); // 1.05
        PwmActor garaz3PwmActor = addLddLight(pwmActors, "pwmG3", "Gar???? 3", lddDevice9.getLdd5(), 0.6, garazZadveriSwAIndicator, garazGarazSwAIndicator); // .6
        PwmActor drevnikPwmActor = addLddLight(pwmActors, "pwmDrv", "D??evn??k", lddDevice9.getLdd6(), 0.7, drevnikSwIndicator); // .72

        //groups
        IOnOffActor[] svetlaDoleVzadu = {chodbaDolePwmActor, zadveriDolePwmActor,
                pradelna1PwmActor, pradelna2PwmActor, pradelna3PwmActor,
                spajzPwmActor, koupelnaDolePwmActor,
                koupelnaDoleZrcadlaPwmActor, terasaPwmActor,
                svSklepLevy, svSklepPravy, drevnikPwmActor};

        IOnOffActor[] svetlaDole = {kuchyn1PwmActor, kuchyn2PwmActor, kuchyn3PwmActor, kuchyn4PwmActor, kuchyn5PwmActor,
                jidelna1PwmActor, jidelna2PwmActor, jidelna3PwmActor, jidelnaStulPwmActor,
                obyvak01PwmActor, obyvak02PwmActor, obyvak03PwmActor, obyvak04PwmActor, obyvak05PwmActor,
                obyvak06PwmActor, obyvak07PwmActor, obyvak08PwmActor, /*obyvak09PwmActor,*/ obyvak10PwmActor,
                obyvak11PwmActor, obyvak12PwmActor, obyvak13PwmActor, kuchynLinkaPwmActor};

        IOnOffActor[] svetlaHoreVenku = {garaz1PwmActor, garaz2PwmActor, garaz3PwmActor, vchodHorePwmActor, pudaPwmActor,
                zadveriPwmActor};
        IOnOffActor[] svetlaHore = {vratnice1PwmActor, vratnice2PwmActor, koupelnaPwmActor, koupelnaZrcadlaPwmActor,
                chodbaUPokojuPwmActor, chodbaSchodyPwmActor, schodyPwmActor, wcPwmActor, satnaPwmActor,
                krystofPwmActor, pataPwmActor, marekPwmActor, loznice1PwmActor, loznice2PwmActor, pracovnaPwmActor};

        // koupelna
        configurePwmLights(lst, koupelnaHoreSw1, WallSwitch.Side.LEFT, 0.5, koupelnaZrcadlaPwmActor);
        configurePwmLights(lst, koupelnaHoreSw1, WallSwitch.Side.RIGHT, 0.25, koupelnaPwmActor);

        configureLouvers(lst, koupelnaHoreSw2, WallSwitch.Side.LEFT, zaluzieKoupelna);
        lst.addActionBinding(new ActionBinding(koupelnaHoreSw2.getRightUpperButton(), null, new Action[]{
                new SwitchOnSensorAction(zaricKoupelnaHore1Trubice, 900, 1.0, AbstractSensorAction.Priority.LOW, durationInfra),
                new SwitchOnSensorAction(zaricKoupelnaHore2Trubice, 900, 1.0, AbstractSensorAction.Priority.LOW, durationInfra),
                new SwitchOnSensorAction(hvacActor, 1800, 1.0, AbstractSensorAction.Priority.LOW, durationHvac)
        }));
        lst.addActionBinding(new ActionBinding(koupelnaHoreSw2.getRightBottomButton(), null, new Action[]{
                new SwitchOffAction(zaricKoupelnaHore1Trubice, durationInfra),
                new SwitchOffAction(zaricKoupelnaHore2Trubice, durationInfra),
                new SwitchOffAction(hvacActor, durationHvac)
        }));

        // koupelna u okna
        configureLouvers(lst, koupelnaHoreOknoSw, WallSwitch.Side.LEFT, zaluzieKoupelna);
        lst.addActionBinding(new ActionBinding(koupelnaHoreOknoSw.getRightUpperButton(), new Action[]{
                new SwitchOnSensorAction(zaricKoupelnaHore1Trubice, 900, 1.0),
                new SwitchOnSensorAction(zaricKoupelnaHore2Trubice, 900, 1.0)}, null));
        lst.addActionBinding(new ActionBinding(koupelnaHoreOknoSw.getRightBottomButton(), new Action[]{
                new SwitchOffAction(zaricKoupelnaHore1Trubice), new SwitchOffAction(zaricKoupelnaHore2Trubice)}, null));


        // kuchyn + obyvak
//        lst.addActionBinding(new ActionBinding(schodyDoleL2Sw.getLeftBottomButton(), new InvertAction(recuperation), null));

        configureLouvers(lst, schodyDoleR1Sw, WallSwitch.Side.LEFT, zaluzieKuchyn);
        configureLouvers(lst, schodyDoleR1Sw, WallSwitch.Side.RIGHT, zaluzieObyvak1);
        configureLouvers(lst, schodyDoleR2Sw, WallSwitch.Side.LEFT, zaluzieObyvak2, zaluzieObyvak3);
        configureLouvers(lst, schodyDoleR2Sw, WallSwitch.Side.RIGHT, zaluzieObyvak4);
        configureLouvers(lst, schodyDoleR3Sw, WallSwitch.Side.LEFT, zaluzieObyvak5, zaluzieObyvak6);
        configurePwmLights(lst, schodyDoleR3Sw, WallSwitch.Side.RIGHT, 0.66, jidelnaStulPwmActor);

        // obyvak u schodu
        SwitchAllOffWithMemory allLightsFromKitchenToLivingRoomOff = new SwitchAllOffWithMemory(svetlaDole);
        lst.addActionBinding(new ActionBinding(schodyDoleL1Sw.getLeftBottomButton(),
                allLightsFromKitchenToLivingRoomOff, null));


        SwitchOnSensorAction bzucakAction = new SwitchOnSensorAction(bzucakDvere, 5);
        lst.addActionBinding(new ActionBinding(schodyDoleL1Sw.getRightUpperButton(), bzucakAction, null));

        Action stromekAction = new InvertActionWithTimer(zasStromek, 4 * 3600);
        Action malyStromekAction = new InvertActionWithTimer(malyStromek, 4 * 3600);
        lst.addActionBinding(new ActionBinding(schodyDoleL1Sw.getRightBottomButton(), new Action[]{stromekAction, malyStromekAction}, null));

        IndicatorAction garazIndicator = new IndicatorAction(schodyDoleR1Sw.getRedLedIndicator(SwitchIndicator.Mode.SIGNAL_ALL_OFF));


        // gauc
        lst.addActionBinding(new ActionBinding(obyvakGaucLSw.getLeftBottomButton(), allLightsFromKitchenToLivingRoomOff, null));
        configurePwmLights(lst, obyvakGaucLSw, WallSwitch.Side.RIGHT, 0.7, /*obyvak09PwmActor,*/ obyvak12PwmActor, obyvak13PwmActor);
        configureLouvers(lst, obyvakGaucRSw, WallSwitch.Side.LEFT, zaluzieObyvak4);
        configureLouvers(lst, obyvakGaucRSw, WallSwitch.Side.RIGHT, zaluzieObyvak5, zaluzieObyvak6);


        // obyvak vzadu
        lst.addActionBinding(new ActionBinding(obyvakVzadu1Sw.getLeftBottomButton(),
                allLightsFromKitchenToLivingRoomOff, null));

        configureLouvers(lst, obyvakVzadu2Sw, WallSwitch.Side.LEFT, zaluzieObyvak2);
        configureLouvers(lst, obyvakVzadu2Sw, WallSwitch.Side.RIGHT, zaluzieObyvak3);

        configureLouvers(lst, obyvakVzadu3Sw, WallSwitch.Side.LEFT, zaluzieObyvak4);
        configureLouvers(lst, obyvakVzadu3Sw, WallSwitch.Side.RIGHT, zaluzieObyvak5, zaluzieObyvak6);

        configurePwmLights(lst, obyvakVzadu4Sw, WallSwitch.Side.LEFT, 0.7, obyvak01PwmActor, obyvak02PwmActor, obyvak03PwmActor);
        configurePwmLights(lst, obyvakVzadu4Sw, WallSwitch.Side.RIGHT, 0.7, /*obyvak09PwmActor,*/ obyvak12PwmActor, obyvak13PwmActor);


        // wc
        configurePwmLights(lst, wcSw, WallSwitch.Side.LEFT, 0.6, wcPwmActor);
        configurePwmLights(lst, wcSw, WallSwitch.Side.RIGHT, 0.6, wcPwmActor);


        // svetla satna
        configurePwmLights(lst, satnaSw3, WallSwitch.Side.RIGHT, 0.8, satnaPwmActor);
        configureLouvers(lst, satnaSw3, WallSwitch.Side.LEFT, zaluzieSatna);

        // zadveri
        configurePwmLights(lst, zadveriSwA1, WallSwitch.Side.LEFT, 0.8, zadveriPwmActor);

        SwitchOnSensorAction ovladacGarazAction = new SwitchOnSensorAction(ovladacGaraz, 1);
//        InvertActionWithTimer stomekAction = new InvertActionWithTimer(zasStromek, 12600);
        lst.addActionBinding(new ActionBinding(zadveriSwA1.getRightUpperButton(), ovladacGarazAction, null));
//        lst.addActionBinding(new ActionBinding(zadveriSwA1.getRightBottomButton(), stomekAction, null));

        configurePwmLights(lst, zadveriSwA2, WallSwitch.Side.LEFT, 0.5, garaz1PwmActor, garaz2PwmActor);
        configurePwmLights(lst, zadveriSwA2, WallSwitch.Side.RIGHT, 0.8, garaz3PwmActor);
        configurePwmLights(lst, zadveriVratniceSw3, WallSwitch.Side.LEFT, 0.8, vchodHorePwmActor);
        configurePwmLights(lst, zadveriVratniceSw3, WallSwitch.Side.RIGHT, 0.8, zadveriPwmActor);

        // zadveri venku
        SwitchOnSensorAction zvonekAction = new SwitchOnSensorAction(obyvakZasLZvonek, 5);
        lst.addActionBinding(new ActionBinding(zvonekLevySw.getLeftUpperButton(), zvonekAction, null));
        lst.addActionBinding(new ActionBinding(zvonekLevySw.getRightUpperButton(), zvonekAction, null));
        lst.addActionBinding(new ActionBinding(zvonekLevySw.getLeftBottomButton(), zvonekAction, null));
        lst.addActionBinding(new ActionBinding(zvonekLevySw.getRightBottomButton(), zvonekAction, null));
        lst.addActionBinding(new ActionBinding(zvonekPravySw.getLeftUpperButton(), zvonekAction, null));
        lst.addActionBinding(new ActionBinding(zvonekPravySw.getRightUpperButton(), zvonekAction, null));
        lst.addActionBinding(new ActionBinding(zvonekPravySw.getLeftBottomButton(), zvonekAction, null));
        lst.addActionBinding(new ActionBinding(zvonekPravySw.getRightBottomButton(), zvonekAction, null));

        // garaz
        configurePwmLights(lst, garazASw1, WallSwitch.Side.LEFT, 0.5, garaz1PwmActor, garaz2PwmActor);
        configurePwmLights(lst, garazASw1, WallSwitch.Side.RIGHT, 0.8, garaz3PwmActor);
        lst.addActionBinding(new ActionBinding(garazASw2.getLeftUpperButton(), ovladacGarazAction, null));
        lst.addActionBinding(new ActionBinding(garazASw2.getLeftBottomButton(), ovladacGarazAction, null));
        lst.addActionBinding(new ActionBinding(garazASw2.getRightUpperButton(), new SwitchOnSensorAction(pudaPwmActor, 1200), null));
        lst.addActionBinding(new ActionBinding(garazASw2.getRightBottomButton(), new SwitchOffAction(pudaPwmActor), null));

        configurePwmLights(lst, garazBSwL, WallSwitch.Side.LEFT, 0.8, garaz3PwmActor);
        configurePwmLights(lst, garazBSwL, WallSwitch.Side.RIGHT, 0.5, garaz2PwmActor);
        configurePwmLights(lst, garazBSwR, WallSwitch.Side.LEFT, 0.5, garaz1PwmActor);
        lst.addActionBinding(new ActionBinding(garazBSwR.getRightUpperButton(), ovladacGarazAction, null));
        lst.addActionBinding(new ActionBinding(garazBSwR.getRightBottomButton(), ovladacGarazAction, null));

        // Krystof + Pata
        configureLouvers(lst, krystofSwA1, WallSwitch.Side.LEFT, zaluziePata);
        configureLouvers(lst, krystofSwA1, WallSwitch.Side.RIGHT, zaluzieKrystof);
        configurePwmLights(lst, krystofSwA2, WallSwitch.Side.LEFT, 0.5, pataPwmActor);
        configurePwmLights(lst, krystofSwA2, WallSwitch.Side.RIGHT, 0.5, krystofPwmActor);

        configurePwmLights(lst, krystofPostelSw, WallSwitch.Side.LEFT, 0.5, krystofPwmActor);
        configureLouvers(lst, krystofPostelSw, WallSwitch.Side.RIGHT, zaluzieKrystof, zaluziePata);

        configureLouvers(lst, patrikSw1, WallSwitch.Side.LEFT, zaluziePata);
        configureLouvers(lst, patrikSw1, WallSwitch.Side.RIGHT, zaluzieKrystof);
        configurePwmLights(lst, patrikSw2, WallSwitch.Side.LEFT, 0.5, pataPwmActor);
        configurePwmLights(lst, patrikSw2, WallSwitch.Side.RIGHT, 0.5, krystofPwmActor);

        configureLouvers(lst, patrikPostelSw3, WallSwitch.Side.LEFT, zaluziePata, zaluzieKrystof);
        configurePwmLights(lst, patrikPostelSw3, WallSwitch.Side.RIGHT, 0.5, pataPwmActor);

        // Marek
        configureLouvers(lst, marekSwA1, WallSwitch.Side.LEFT, zaluzieMarek);
        configureLouvers(lst, marekSwA1, WallSwitch.Side.RIGHT, zaluzieMarek);
        configurePwmLights(lst, marekSwA2, WallSwitch.Side.LEFT, 0.5, marekPwmActor);
        configurePwmLights(lst, marekSwA2, WallSwitch.Side.RIGHT, 0.5, marekPwmActor);
        configureLouvers(lst, marekPostelSw3, WallSwitch.Side.LEFT, zaluzieMarek);
        configurePwmLights(lst, marekPostelSw3, WallSwitch.Side.RIGHT, 0.5, marekPwmActor);


        // chodba hore - koupelna
        configurePwmLights(lst, chodbaHoreKoupelnaSw3, WallSwitch.Side.LEFT, 0.8, zadveriPwmActor);
        configurePwmLights(lst, chodbaHoreKoupelnaSw3, WallSwitch.Side.RIGHT, 0.4, chodbaSchodyPwmActor, chodbaUPokojuPwmActor);
        // switch off 4 lights
//        lst.addActionBinding(new ActionBinding(chodbaHoreKoupelnaSw3.getRightBottomButton(),
//                new Action[]{new SwitchOffAction(pataPwmActor), new SwitchOffAction(krystofPwmActor),
//                        new SwitchOffAction(marekPwmActor), new SwitchOffAction(satnaPwmActor)}, null));

        // chodba hore - krystof
        configurePwmLights(lst, chodbaHoreKrystofSwA3, WallSwitch.Side.LEFT, 0.4, chodbaUPokojuPwmActor, chodbaSchodyPwmActor);
        configurePwmLights(lst, chodbaHoreKrystofSwA3, WallSwitch.Side.RIGHT, 0.4, chodbaUPokojuPwmActor, chodbaSchodyPwmActor);

        // chodba hore - patrik
        configurePwmLights(lst, chodbaHorePatrikSw3, WallSwitch.Side.LEFT, 0.4, chodbaUPokojuPwmActor, chodbaSchodyPwmActor);
        configurePwmLights(lst, chodbaHorePatrikSw3, WallSwitch.Side.RIGHT, 0.8, satnaPwmActor);

        // chodba hore - u satny
        configurePwmLights(lst, chodbaVzaduLSw, WallSwitch.Side.LEFT, 0.8, satnaPwmActor);
        configurePwmLights(lst, chodbaVzaduLSw, WallSwitch.Side.RIGHT, 0.4, chodbaUPokojuPwmActor, chodbaSchodyPwmActor);
        configureLouvers(lst, chodbaVzaduRSw, WallSwitch.Side.LEFT, zaluzieSatna);
        configureLouvers(lst, chodbaVzaduRSw, WallSwitch.Side.RIGHT, zaluzieChodba2, zaluzieChodba1);

        // chodba hore - u okna
        configureLouvers(lst, chodbaOkno1Sw, WallSwitch.Side.LEFT, zaluzieChodba1);
        configureLouvers(lst, chodbaOkno1Sw, WallSwitch.Side.RIGHT, zaluzieChodba2);
        configurePwmLights(lst, chodbaOkno2Sw, WallSwitch.Side.LEFT, 0.4, chodbaUPokojuPwmActor, chodbaSchodyPwmActor);
        configurePwmLights(lst, chodbaOkno2Sw, WallSwitch.Side.RIGHT, 0.15, schodyPwmActor);

        lst.addActionBinding(new ActionBinding(chodbaOkno3Sw.getLeftBottomButton(),
                allLightsFromKitchenToLivingRoomOff, null));
        lst.addActionBinding(new ActionBinding(chodbaOkno3Sw.getRightBottomButton(),
                SwitchAllOffWithMemory.createSwitchOffActions(svetlaDoleVzadu), null));
        lst.addActionBinding(new ActionBinding(chodbaOkno3Sw.getLeftUpperButton(),
                SwitchAllOffWithMemory.createSwitchOffActions(svetlaHore), null));
        lst.addActionBinding(new ActionBinding(chodbaOkno3Sw.getRightUpperButton(),
                SwitchAllOffWithMemory.createSwitchOffActions(svetlaHoreVenku), null));

        // loznice
        configureLouvers(lst, lozniceOknoSw1, WallSwitch.Side.LEFT, zaluzieLoznice1);
        configureLouvers(lst, lozniceOknoSw1, WallSwitch.Side.RIGHT, zaluzieLoznice2);
        configureLouvers(lst, lozniceDvereSw1, WallSwitch.Side.LEFT, zaluzieLoznice1);
        configureLouvers(lst, lozniceDvereSw1, WallSwitch.Side.RIGHT, zaluzieLoznice2);
        configurePwmLights(lst, lozniceDvereSw2, WallSwitch.Side.LEFT, 0.4, loznice2PwmActor);
        configurePwmLights(lst, lozniceDvereSw2, WallSwitch.Side.RIGHT, 0.4, loznice1PwmActor);
        configurePwmLights(lst, lozniceOknoSw2, WallSwitch.Side.LEFT, 0.4, loznice1PwmActor);
        configurePwmLights(lst, lozniceOknoSw2, WallSwitch.Side.RIGHT, 0.4, loznice2PwmActor);

        configureLouvers(lst, lozniceZedSw1, WallSwitch.Side.LEFT, zaluzieLoznice1);
        configureLouvers(lst, lozniceZedSw1, WallSwitch.Side.RIGHT, zaluzieLoznice2);
        configurePwmLights(lst, lozniceZedSw2, WallSwitch.Side.LEFT, 0.4, loznice1PwmActor);
        configurePwmLights(lst, lozniceZedSw2, WallSwitch.Side.RIGHT, 0.4, loznice2PwmActor);
        configurePwmLights(lst, lozniceZedLampySw, WallSwitch.Side.LEFT, 0.4, loznice1PwmActor);
        configurePwmLights(lst, lozniceZedLampySw, WallSwitch.Side.RIGHT, 0.4, loznice2PwmActor);

        //pracovna
        configureLouvers(lst, pracovnaSw2, WallSwitch.Side.LEFT, zaluziePracovna);
        configurePwmLights(lst, pracovnaSw2, WallSwitch.Side.RIGHT, 0.3, pracovnaPwmActor);

        // vratnice

        //TODO: Remove test 41
        WallSwitch test41Sw1 = new WallSwitch("Test41.1", switchTestNode41, 1);
        configureLouvers(lst, test41Sw1, WallSwitch.Side.RIGHT, zaluzieVratnice1);

        configureLouvers(lst, vratniceSw1, WallSwitch.Side.RIGHT, zaluzieVratnice1);
        configureLouvers(lst, vratniceSw2, WallSwitch.Side.LEFT, zaluzieVratnice2);
        configureLouvers(lst, vratniceSw2, WallSwitch.Side.RIGHT, zaluzieVratnice3);
        configurePwmLights(lst, vratniceSw1, WallSwitch.Side.LEFT, 0.4, vratnice1PwmActor, vratnice2PwmActor);

        // sklepy
        //    - zadveri
        lst.addActionBinding(new ActionBinding(zadveriDoleVchodRSw.getLeftUpperButton(), new SwitchOnSensorAction(svSklepLevy, 1800), null));
        lst.addActionBinding(new ActionBinding(zadveriDoleVchodRSw.getLeftBottomButton(), new SwitchOffAction(svSklepLevy), null));

        lst.addActionBinding(new ActionBinding(zadveriDoleVchodRSw.getRightUpperButton(), new SwitchOnSensorAction(svSklepPravy, 1800), null));
        lst.addActionBinding(new ActionBinding(zadveriDoleVchodRSw.getRightBottomButton(), new SwitchOffAction(svSklepPravy), null));


        //    - venku Levy
        configurePwmLights(lst, sklepLevyLSw, WallSwitch.Side.LEFT, 0.8, drevnikPwmActor);
        configurePwmLights(lst, sklepLevyLSw, WallSwitch.Side.RIGHT, 0.4, terasaPwmActor);

        lst.addActionBinding(new ActionBinding(sklepLevyRSw.getLeftUpperButton(), new SwitchOnSensorAction(svSklepLevy, 1800), null));
        lst.addActionBinding(new ActionBinding(sklepLevyRSw.getLeftBottomButton(), new SwitchOffAction(svSklepLevy), null));
        lst.addActionBinding(new ActionBinding(sklepLevyRSw.getRightUpperButton(), new SwitchOnSensorAction(svSklepLevy, 1800), null));
        lst.addActionBinding(new ActionBinding(sklepLevyRSw.getRightBottomButton(), new SwitchOffAction(svSklepLevy), null));

        //    - venku pravy
        configurePwmLights(lst, sklepPravySw, WallSwitch.Side.LEFT, 0.4, terasaPwmActor);
        lst.addActionBinding(new ActionBinding(sklepPravySw.getRightUpperButton(), new SwitchOnSensorAction(svSklepPravy, 1800), null));
        lst.addActionBinding(new ActionBinding(sklepPravySw.getRightBottomButton(), new SwitchOffAction(svSklepPravy), null));


        // zadveri dole
        configurePwmLightsImpl(lst, zadveriDolePradelnaSw, WallSwitch.Side.LEFT, 0.8, new PwmActor[]{pradelna2PwmActor}, new PwmActor[]{pradelna3PwmActor});
        configurePwmLightsImpl(lst, zadveriDolePradelnaSw, WallSwitch.Side.RIGHT, 0.8, new PwmActor[]{pradelna1PwmActor}, new PwmActor[]{pradelna3PwmActor});

        configurePwmLights(lst, zadveriDoleChodbaSw, WallSwitch.Side.LEFT, 0.6, chodbaDolePwmActor);
        configurePwmLights(lst, zadveriDoleChodbaSw, WallSwitch.Side.RIGHT, 0.6, zadveriDolePwmActor);

        configurePwmLights(lst, zadveriDoleVchodLSw, WallSwitch.Side.LEFT, 0.6, zadveriDolePwmActor);
        configurePwmLights(lst, zadveriDoleVchodLSw, WallSwitch.Side.RIGHT, 0.6, terasaPwmActor);

        // chodba dole
        configurePwmLights(lst, chodbaDoleSpajzSw3, WallSwitch.Side.LEFT, 0.8, spajzPwmActor);
        configurePwmLights(lst, chodbaDoleSpajzSw3, WallSwitch.Side.RIGHT, 0.4, chodbaDolePwmActor);

        // pradelna
        configurePwmLights(lst, pradelnaSw1, WallSwitch.Side.LEFT, 0.6, pradelna2PwmActor);
        configurePwmLights(lst, pradelnaSw1, WallSwitch.Side.RIGHT, 0.6, pradelna3PwmActor);

//        lst.addActionBinding(new ActionBinding(pradelnaSw1.getLeftUpperButton(), new Action[]{new SwitchOnSensorAction(zasStromek, 1800, 100, AbstractSensorAction.Priority.MEDIUM)}, null));
//        lst.addActionBinding(new ActionBinding(pradelnaSw1.getLeftBottomButton(), new Action[]{new SwitchOffAction(zasStromek)}, null));
//        lst.addActionBinding(new ActionBinding(pradelnaSw1.getRightUpperButton(), new Action[]{new SwitchOnSensorAction(zasStromek, 1800, 100, AbstractSensorAction.Priority.MEDIUM)}, null));
//        lst.addActionBinding(new ActionBinding(pradelnaSw1.getRightBottomButton(), new Action[]{new SwitchOffAction(zasStromek)}, null));

        //koupelna dole
        configurePwmLights(lst, koupelnaDoleSw2, WallSwitch.Side.LEFT, 0.4, koupelnaDolePwmActor);
        configurePwmLights(lst, koupelnaDoleSw2, WallSwitch.Side.RIGHT, 0.8, koupelnaDoleZrcadlaPwmActor);
        configureLouvers(lst, koupelnaDoleSw1, WallSwitch.Side.RIGHT, zaluzieKoupelnaDole);

        // kuchyn
        lst.addActionBinding(new ActionBinding(getBottomButton(kuchynLSw1, WallSwitch.Side.LEFT),
                SwitchAllOffWithMemory.createSwitchOffActions(svetlaDoleVzadu), null));

        RadioOnOffActor radioActor = new RadioOnOffActor(OptionsSingleton.get("mpd.radio.ip"), OptionsSingleton.get("mpd.radio.stream.url"));
        lst.addActionBinding(new ActionBinding(getUpperButton(kuchynLSw1, WallSwitch.Side.LEFT),
                new InvertAction(radioActor), null));

        configurePwmLights(lst, kuchynLSw3, WallSwitch.Side.RIGHT, 0.75, kuchynLinkaPwmActor);
        configurePwmLights(lst, kuchynLSw3, WallSwitch.Side.LEFT, 0.5, kuchyn1PwmActor, kuchyn2PwmActor, kuchyn3PwmActor);
        configurePwmLights(lst, kuchynLSw2, WallSwitch.Side.RIGHT, 0.5, obyvak06PwmActor, jidelna1PwmActor, kuchyn5PwmActor);
        configurePwmLights(lst, kuchynLSw2, WallSwitch.Side.LEFT, 0.7, obyvak01PwmActor, obyvak10PwmActor, obyvak13PwmActor);

        configurePwmLights(lst, kuchynRSw1, WallSwitch.Side.RIGHT, 0.75, kuchynLinkaPwmActor);
        configurePwmLights(lst, kuchynRSw1, WallSwitch.Side.LEFT, 0.5, kuchyn1PwmActor, kuchyn2PwmActor, kuchyn3PwmActor);

        SunCondition sunCondition = new SunCondition(0, -15);
        DarkCondition corridorDarkCondition = new DarkCondition(sunCondition, new IOnOffActor[]{chodbaUPokojuPwmActor, chodbaUPokojuPwmActor, obyvak01PwmActor, obyvak02PwmActor, obyvak03PwmActor,
                obyvak04PwmActor, obyvak05PwmActor, obyvak06PwmActor, obyvak07PwmActor, obyvak08PwmActor, /*obyvak09PwmActor,*/ obyvak10PwmActor, obyvak11PwmActor, obyvak12PwmActor,
                obyvak13PwmActor, jidelna1PwmActor, jidelna2PwmActor, jidelna3PwmActor,
                vchodHorePwmActor});

        // PIRs
        InputDevice pirA1Prizemi = new InputDevice("pirA1Prizemi", pirNodeA, 1);
        setupPir(lst, pirA1Prizemi.getIn1AndActivate(), "pirPrdDv", "Pradelna dvere", new SwitchOnSensorAction(pradelna1PwmActor, 600, 0.8), new SwitchOffSensorAction(pradelna1PwmActor, 60));
        setupPir(lst, pirA1Prizemi.getIn2AndActivate(), "pirPrdPr", "Pradelna pracka", new SwitchOnSensorAction(pradelna1PwmActor, 600, 0.8), new SwitchOffSensorAction(pradelna1PwmActor, 60));
        setupMagneticSensor(lst, pirA1Prizemi.getIn3AndActivate(), "pisD", "Pisoar Dole",
                new Action[]{new SwitchOnSensorAction(pisoarDole, 3)},
                new Action[]{new SwitchOnSensorAction(pisoarDole, 7)});
        setupPir(lst, pirA1Prizemi.getIn4AndActivate(), "pirVchH", "Vchod hore", new SwitchOnSensorAction(vchodHorePwmActor, 600, 0.8, sunCondition), new SwitchOffSensorAction(vchodHorePwmActor, 60));
        setupPir(lst, pirA1Prizemi.getIn5AndActivate(), "pirSch", "Schodiste", new SwitchOnSensorAction(schodyPwmActor, 600, 0.15, corridorDarkCondition), new SwitchOffSensorAction(schodyPwmActor, 30));
        setupMagneticSensor(lst, pirA1Prizemi.getIn6AndActivate(), "pisH", "Pisoar Hore",
                new Action[]{new SwitchOnSensorAction(pisoarHore, 3)},
                new Action[]{new SwitchOnSensorAction(pisoarHore, 7)});
        // A6:3 "zadveri venku - spinac puda"

        InputDevice pirA2Patro = new InputDevice("pirA2Patro", pirNodeA, 2);
        setupPir(lst, pirA2Patro.getIn1AndActivate(), "pirChWc", "Chodba pred WC", new SwitchOnSensorAction(schodyPwmActor, 600, 0.15, corridorDarkCondition), new SwitchOffSensorAction(schodyPwmActor, 30));
        setupPir(lst, pirA2Patro.getIn2AndActivate(), "pirCh", "Chodba", new SwitchOnSensorAction(schodyPwmActor, 600, 0.15, corridorDarkCondition), new SwitchOffSensorAction(schodyPwmActor, 30));
        setupPir(lst, pirA2Patro.getIn3AndActivate(), "pirWc", "WC",
                new Action[]{new SwitchOnSensorAction(wcPwmActor, 600, 1.0, sunCondition)},
                new Action[]{new SwitchOffSensorAction(wcPwmActor, 60)});
        setupPir(lst, pirA2Patro.getIn5AndActivate(), "pirZadHVch", "Zadveri hore vchod", new SwitchOnSensorAction(zadveriPwmActor, 600, 1.0, sunCondition), new SwitchOffSensorAction(zadveriPwmActor, 15));
        setupPir(lst, pirA2Patro.getIn6AndActivate(), "pirZadHCh", "Zadveri hore chodba", new SwitchOnSensorAction(zadveriPwmActor, 600, 1.0, sunCondition), new SwitchOffSensorAction(zadveriPwmActor, 15));
        setupPir(lst, pirA2Patro.getIn4AndActivate(), "pirChMa", "Chodba nad Markem", (Action) null, null);

        InputDevice pirA3Prizemi = new InputDevice("pirA3Prizemi", pirNodeA, 3);
        setupPir(lst, pirA3Prizemi.getIn1AndActivate(), "pirJid", "Jidelna", (Action) null, null);
        setupPir(lst, pirA3Prizemi.getIn2AndActivate(), "pirObyv", "Obyvak", (Action) null, null);
        setupPir(lst, pirA3Prizemi.getIn3AndActivate(), "pirChD", "Chodba dole", new SwitchOnSensorAction(chodbaDolePwmActor, 600, 1.0, sunCondition), new SwitchOffSensorAction(chodbaDolePwmActor, 15));
        setupPir(lst, pirA3Prizemi.getIn4AndActivate(), "pirKoupD", "Koupelna dole", new SwitchOnSensorAction(koupelnaDolePwmActor, 600, 0.5, sunCondition), new SwitchOffSensorAction(koupelnaDolePwmActor, 60));
        setupPir(lst, pirA3Prizemi.getIn5AndActivate(), "pirSpa", "Spajza", new SwitchOnSensorAction(spajzPwmActor, 600, 1.0), new SwitchOffSensorAction(spajzPwmActor, 20));
        setupPir(lst, pirA3Prizemi.getIn6AndActivate(), "pirZadD", "Zadveri dole", new SwitchOnSensorAction(zadveriDolePwmActor, 600, 1.0, new SunCondition(-15, -30)), new SwitchOffSensorAction(zadveriDolePwmActor, 15));

        InputDevice cidlaGaraz = new InputDevice("cidlaGaraz", garazVzadu, 3);
        setupMagneticSensor(lst, cidlaGaraz.getIn1AndActivate(), "mgntGH", "Garaz hore", (Action) null, null);
        setupMagneticSensor(lst, cidlaGaraz.getIn2AndActivate(), "mgntGD", "Garaz dole", garazIndicator.getOnAction(), garazIndicator.getOffAction());

        WaterPumpMonitor waterPumpMonitor = new WaterPumpMonitor();
        InputDevice cidlaRozvadec = new InputDevice("cidlaRozvadec", rozvadecDole, 3);
        setupMagneticSensor(lst, cidlaRozvadec.getIn1AndActivate(), "mgntCrpd", "Cerpadlo", waterPumpMonitor.getOnAction(), waterPumpMonitor.getOffAction());

        List<ServletAction> servletActions = new ArrayList<>();
        servletActions.add(new ServletAction("openDoor", "Bzu????k", bzucakAction));
        servletActions.add(new ServletAction("openGarage", "Gar????", ovladacGarazAction));

        //test wall switch application
        WallSwitch testSw = new WallSwitch("testSwA", switchTestNode50, 1);
        WallSwitch test3Sw = new WallSwitch("test3Sw", switchTestNode50, 3);
        TestingOnOffActor testingRightOnOffActor = new TestingOnOffActor("RightSwitchTestingActor", null, 0, 1, testSw.getRedLedIndicator(SwitchIndicator.Mode.SIGNAL_ALL_OFF), test3Sw.getRedLedIndicator(SwitchIndicator.Mode.SIGNAL_ALL_OFF));
        TestingOnOffActor testingLeftOnOffActor = new TestingOnOffActor("LeftSwitchTestingActor", null, 0, 1, testSw.getGreenLedIndicator(SwitchIndicator.Mode.SIGNAL_ALL_OFF), test3Sw.getGreenLedIndicator(SwitchIndicator.Mode.SIGNAL_ALL_OFF));
        lst.addActionBinding(new ActionBinding(testSw.getRightBottomButton(), new SwitchOffAction(testingRightOnOffActor), null));
        lst.addActionBinding(new ActionBinding(testSw.getRightUpperButton(), new SwitchOnAction(testingRightOnOffActor), null));
        lst.addActionBinding(new ActionBinding(testSw.getLeftUpperButton(), new SwitchOnAction(testingLeftOnOffActor), null));
        lst.addActionBinding(new ActionBinding(testSw.getLeftBottomButton(), new SwitchOffAction(testingLeftOnOffActor), null));

        List<WebSocketHandler> wsHandlers = new ArrayList<>();
        // page handlers
        Page floorsPage = new StaticPage("/", "/floorPlan.html", "Mapa");
        List<Page> pages = new ArrayList<>();
        //noinspection CollectionAddAllCanBeReplacedWithConstructor
        pages.addAll(Arrays.asList(
                floorsPage,
                new LightsPage(pwmActors, pages),
                new LouversPage(louversControllers, pages),
                new PirPage(pirStatusList, pages),
                new NodeInfoPage(nodeInfoRegistry, pages, servletActions),
                new OptionsPage(OptionsSingleton.getInstance(), pages)));
        // rest handlers
        List<StatusHandler> deviceRestHandlers = Arrays.asList(
                new LouversHandler(Arrays.asList(louversControllers)),
                new AirValveHandler(Arrays.asList(valveControllers)),
                new PwmLightsHandler(pwmActors),
                new PirHandler(pirStatusList),
                new WaterPumpHandler(Collections.singleton(waterPumpMonitor)),
                new HvacHandler(Collections.singleton(hvacActor)));
//        configureSimulator(pages, wsHandlers, false);
        // rest/all handler
        List<Handler> handlers = new ArrayList<>();
        handlers.add(new NodeInfoDetailPage(nodeInfoRegistry, pages));
        handlers.addAll(pages);
        handlers.add(new StaticPage(VIRTUAL_CONFIGURATION_JS_FILENAME, "/configuration-pi.js", null));
        handlers.add(new GetBackendUrlJs());
        handlers.add(new NodeHandler(nodeInfoRegistry));
        handlers.add(new ServletActionHandler(servletActions));
        handlers.addAll(deviceRestHandlers);
        handlers.add(new AllStatusHandler(deviceRestHandlers));
        servlet = new Servlet(handlers, floorsPage.getPath(), wsHandlers);

//        OnOffActor testLedActor = new OnOffActor("testLed", testOutputDevice3.getOut2(), 1, 0);
//        lst.addActionBinding(new ActionBinding(testInputDevice2.getIn1(), new Action[]{new SensorAction(testLedActor, 10)}, new Action[]{new SensorAction(testLedActor, 60)}));
    }

    private HvacDevice startHvacDevice() {
        HvacDevice hvacDevice = new HvacDevice("/dev/ttyUSB0", 0x85, 0x20, null);
        try {
            hvacDevice.start();
        } catch (IOException e) {
            log.error("Failed to start HVAC Device", e);
            return null;
        }
        return hvacDevice;
    }
}
