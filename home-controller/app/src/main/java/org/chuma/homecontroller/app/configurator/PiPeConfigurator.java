package org.chuma.homecontroller.app.configurator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
import org.chuma.homecontroller.app.servlet.rest.AllStatusHandler;
import org.chuma.homecontroller.app.servlet.rest.ElectricitySpotPriceHandler;
import org.chuma.homecontroller.app.servlet.rest.InverterHandler;
import org.chuma.homecontroller.app.servlet.rest.LouversHandler;
import org.chuma.homecontroller.app.servlet.rest.NodeHandler;
import org.chuma.homecontroller.app.servlet.rest.OnOffHandler;
import org.chuma.homecontroller.app.servlet.rest.PirHandler;
import org.chuma.homecontroller.app.servlet.rest.PwmLightsHandler;
import org.chuma.homecontroller.app.servlet.rest.ServletActionHandler;
import org.chuma.homecontroller.app.servlet.rest.StatusHandler;
import org.chuma.homecontroller.app.servlet.ws.WebSocketHandler;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.utils.Options;
import org.chuma.homecontroller.base.utils.OptionsSingleton;
import org.chuma.homecontroller.controller.controller.LouversController;
import org.chuma.homecontroller.controller.device.Relay16BoardDevice;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoRegistry;
import org.chuma.homecontroller.controller.persistence.StateMap;
import org.chuma.homecontroller.extensions.external.inverter.ElectricitySpotPriceMonitor;
import org.chuma.homecontroller.extensions.external.inverter.InverterManager;
import org.chuma.homecontroller.extensions.external.inverter.InverterMonitor;
import org.chuma.homecontroller.extensions.external.inverter.impl.SolaxInverterModbusClient;
import org.chuma.homecontroller.extensions.external.inverter.impl.SolaxInverterMonitor;
import org.chuma.hvaccontroller.device.HvacDevice;

@SuppressWarnings({"unused", "DuplicatedCode", "SpellCheckingInspection"})
public class PiPeConfigurator extends AbstractConfigurator {
    static Logger log = LoggerFactory.getLogger(PiConfigurator.class.getName());

    public PiPeConfigurator(NodeInfoRegistry nodeInfoRegistry, StateMap stateMap) {
        super(nodeInfoRegistry, stateMap);
    }

    @Override
    public void configure() {
        Node bridge = nodeInfoRegistry.createNode(1, "Bridge");
        Node rele1 = nodeInfoRegistry.createNode(46, "Relé 1");
        Node rele2 = nodeInfoRegistry.createNode(47, "Relé 2");
        Node rele3 = nodeInfoRegistry.createNode(48, "Relé 3");
        Node krb = nodeInfoRegistry.createNode(38, "Krb");
        Node kuchyn = nodeInfoRegistry.createNode(52, "Kuchyň");
        Node pracovna = nodeInfoRegistry.createNode(53, "Pracovna");
        Node koupelnadole = nodeInfoRegistry.createNode(54, "KoupelnaDole");
        Node loznice = nodeInfoRegistry.createNode(55, "Ložnice");
        Node ochoz = nodeInfoRegistry.createNode(56, "Ochoz");
        Node dada = nodeInfoRegistry.createNode(57, "Dáda");
        Node mates = nodeInfoRegistry.createNode(58, "Mates");
        Node koupelnahore = nodeInfoRegistry.createNode(59, "KoupelnaHore");
        Node juju = nodeInfoRegistry.createNode(60, "Juju");


        Relay16BoardDevice releBoard1 = new Relay16BoardDevice("rele1", rele1);
        Relay16BoardDevice releBoard2 = new Relay16BoardDevice("rele2", rele2);
        Relay16BoardDevice releBoard3 = new Relay16BoardDevice("rele3", rele3);

        LouversController zLoznice = addLouversController("lvLoznice", "Ložnice", releBoard3.getRelay(10), releBoard3.getRelay(9), 57_000);
        LouversController zPracovna = addLouversController("lvPracovna", "Pracovna", releBoard2.getRelay(12), releBoard2.getRelay(11), 38_000);
        LouversController zPracovnaDvere = addLouversController("lvPracovnaDvere", "PracovnaDveře", releBoard3.getRelay(15), releBoard3.getRelay(16), 57_000);
        LouversController zKuchyn = addLouversController("lvKuchyn", "Kuchyň", releBoard2.getRelay(10), releBoard2.getRelay(9), 57_000);
        LouversController zObyvak1 = addLouversController("lvObyvak1", "Obyvák1", releBoard2.getRelay(16), releBoard2.getRelay(15), 46_500);
        LouversController zObyvak2 = addLouversController("lvObyvak2", "Obyvák2", releBoard1.getRelay(1), releBoard1.getRelay(2), 61_000);
        LouversController zObyvak7 = addLouversController("lvObyvak3", "Obyvák3", releBoard2.getRelay(3), releBoard2.getRelay(4), 61_000);
        LouversController zObyvak4 = addLouversController("lvObyvak4", "Obyvák4", releBoard3.getRelay(14), releBoard3.getRelay(13), 25_000);
        LouversController zObyvak5 = addLouversController("lvObyvak5", "Obyvák5", releBoard3.getRelay(12), releBoard3.getRelay(11), 37_000);
        LouversController zObyvak6 = addLouversController("lvObyvak6", "Obyvák6", releBoard2.getRelay(5), releBoard2.getRelay(6), 48_000);
        LouversController zObyvak3 = addLouversController("lvObyvak7", "Obyvák7", releBoard2.getRelay(1), releBoard2.getRelay(2), 59_000);
        LouversController zObyvak8 = addLouversController("lvObyvak8", "Obyvák8", releBoard1.getRelay(3), releBoard1.getRelay(4), 57_000);
        LouversController zSchodiste = addLouversController("lvSchodiste", "Schodiště", releBoard2.getRelay(13), releBoard2.getRelay(14), 45_000);
        LouversController zZpajz = addLouversController("lvSpajz", "Špajz", releBoard3.getRelay(4), releBoard3.getRelay(3), 45_000);
        LouversController zKoupelnaDole = addLouversController("lvKoupelnaDole", "KoupelnaDole", releBoard2.getRelay(7), releBoard2.getRelay(8), 27_000);
        LouversController zDvorek = addLouversController("lvDvorek", "Dvorek", releBoard3.getRelay(1), releBoard3.getRelay(2), 57_000);
        LouversController zDada = addLouversController("lvDada", "Dáda", releBoard1.getRelay(15), releBoard1.getRelay(16), 32_000);
        LouversController zOchoz = addLouversController("lvOchoz", "Ochoz", releBoard1.getRelay(11), releBoard1.getRelay(12), 32_000);
        LouversController zMates2 = addLouversController("lvMates2", "Mates2", releBoard1.getRelay(6), releBoard1.getRelay(5), 32_000);
        LouversController zMates1 = addLouversController("lvMates1", "Mates1", releBoard1.getRelay(8), releBoard1.getRelay(7), 32_000);
        LouversController zKoupelnaHore = addLouversController("lvKoupelnaHore", "KoupelnaHore", releBoard1.getRelay(13), releBoard1.getRelay(14), 28_000);
        LouversController zJuju = addLouversController("lvJuju", "Juju", releBoard1.getRelay(9), releBoard1.getRelay(10), 32_000);

        ElectricitySpotPriceMonitor priceMonitor = new ElectricitySpotPriceMonitor(
                OptionsSingleton.getDouble("electricity.price.distribution-fee"),
                OptionsSingleton.getDouble("electricity.price.sell-fee"),
                OptionsSingleton.getDouble("electricity.price.vat")
        );

        InverterManager inverterManager = null;
        SolaxInverterMonitor inverterMonitor = null;
        try {
            SolaxInverterModbusClient inverterModbusClient = new SolaxInverterModbusClient(OptionsSingleton.get("inverter.ip"));
            inverterMonitor = new SolaxInverterMonitor(inverterModbusClient, 5_000, 60_000);
            inverterMonitor.start();

            inverterManager =new InverterManager(inverterModbusClient, OptionsSingleton.getInstance(), priceMonitor, 0);
        } catch (Exception e) {
            log.error("Failed to init solax inverter client", e);
        }

        List<ServletAction> servletActions = new ArrayList<>();

        //test wall switch application
//        WallSwitch testSw = new WallSwitch("testSwA", switchTestNode, 1, 0.01);
//        servletActions.add(new ServletAction("testRele16-46", "Rele16-46", new Relay16TestLoopAction(new Relay16BoardDevice("test46", rele1))));
//        servletActions.add(new ServletAction("testRele16-47", "Rele16-47", new Relay16TestLoopAction(new Relay16BoardDevice("test47", rele2))));
//        servletActions.add(new ServletAction("testRele16-48", "Rele16-48", new Relay16TestLoopAction(new Relay16BoardDevice("test48", rele3))));

        List<WebSocketHandler> wsHandlers = new ArrayList<>();
        // page handlers
        Page floorsPage = new StaticPage("/", "/floorPlan.html", "Mapa");
        List<Page> pages = new ArrayList<>();
        //noinspection CollectionAddAllCanBeReplacedWithConstructor
        pages.addAll(Arrays.asList(
                floorsPage,
                new LightsPage(lddActors, pages),
                new LouversPage(louversControllers, pages),
                new PirPage(pirStatusList, pages),
                new NodeInfoPage(nodeInfoRegistry, pages, servletActions),
                new OptionsPage(OptionsSingleton.getInstance(), pages)));
        // rest handlers
        List<StatusHandler> deviceRestHandlers = Arrays.asList(
                new LouversHandler(louversControllers),
                new PwmLightsHandler(lddActors),
                new OnOffHandler(onOffActors),
                new PirHandler(pirStatusList),
                new InverterHandler(inverterMonitor),
                new ElectricitySpotPriceHandler(priceMonitor));
//        configureSimulator(pages, wsHandlers, false);
        // rest/all handler
        List<Handler> handlers = new ArrayList<>();
        handlers.add(new NodeInfoDetailPage(nodeInfoRegistry, pages));
        handlers.addAll(pages);
        handlers.add(new StaticPage(VIRTUAL_CONFIGURATION_JS_FILENAME, "/configuration-pipe.js", null));
        handlers.add(new GetBackendUrlJs());
        handlers.add(new NodeHandler(nodeInfoRegistry));
        handlers.add(new ServletActionHandler(servletActions));
        handlers.addAll(deviceRestHandlers);
        handlers.add(new AllStatusHandler(deviceRestHandlers));
        servlet = new Servlet(handlers, floorsPage.getPath(), wsHandlers);

//        OnOffActor testLedActor = new OnOffActor("testLed", testOutputDevice3.getOut2(), 1, 0);
//        lst.addActionBinding(new ActionBinding(testInputDevice2.getIn1(), new Action[]{new SensorAction(testLedActor, 10)}, new Action[]{new SensorAction(testLedActor, 60)}));
    }

    private static SolaxInverterModbusClient createSolaxInverterModbusClient() {
        try {
            final String localIp = OptionsSingleton.getInstance().get("inverter.ip");
            return new SolaxInverterModbusClient(localIp);
        } catch (Exception e) {
            log.error("Failed to init inverter client", e);
            return null;
        }
    }

    @Override
    int getLouversMaxOffsetMs() {
        return 1600;
    }

    private HvacDevice startHvacDevice() {
        String hvacPort = OptionsSingleton.get("hvac.port");
        if (StringUtils.isEmpty(hvacPort)) {
            log.warn("HVAC Device port not set, not starting");
            return null;
        }
        HvacDevice hvacDevice = new HvacDevice(hvacPort, 0x85, 0x20, null);
        try {
            hvacDevice.start();
        } catch (IOException | Error e) {
            log.error("Failed to start HVAC Device", e);
            return null;
        }
        return hvacDevice;
    }
}
