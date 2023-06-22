package org.chuma.homecontroller.app.configurator;

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
import org.chuma.homecontroller.app.servlet.pages.LouversPage;
import org.chuma.homecontroller.app.servlet.pages.NodeInfoDetailPage;
import org.chuma.homecontroller.app.servlet.pages.NodeInfoPage;
import org.chuma.homecontroller.app.servlet.pages.OptionsPage;
import org.chuma.homecontroller.app.servlet.pages.Page;
import org.chuma.homecontroller.app.servlet.pages.StaticPage;
import org.chuma.homecontroller.app.servlet.rest.AllStatusHandler;
import org.chuma.homecontroller.app.servlet.rest.LouversHandler;
import org.chuma.homecontroller.app.servlet.rest.NodeHandler;
import org.chuma.homecontroller.app.servlet.rest.ServletActionHandler;
import org.chuma.homecontroller.app.servlet.rest.StatusHandler;
import org.chuma.homecontroller.app.servlet.ws.WebSocketHandler;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.controller.controller.LouversController;
import org.chuma.homecontroller.controller.device.Relay16BoardDevice;
import org.chuma.homecontroller.controller.device.WallSwitch;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoRegistry;

@SuppressWarnings({"unused", "DuplicatedCode", "SpellCheckingInspection"})
public class OndraConfigurator extends AbstractConfigurator {

    private static final String CFG_INVERTER_MANAGER_HIGH_TARIFF_BATTERY_RESERVE = "inverter.manager.high.tariff.battery.reserve";
    private static final String CFG_INVERTER_MANAGER_HIGH_TARIFF_TIMES = "inverter.manager.high.tariff.times";
    private static final String CFG_INVERTER_MANAGER_MINIMAL_SOC = "inverter.manager.minimal.soc";
    static Logger log = LoggerFactory.getLogger(OndraConfigurator.class.getName());

    public OndraConfigurator(NodeInfoRegistry nodeInfoRegistry) {
        super(nodeInfoRegistry);
    }

    @Override
    public void configure() {
        Node bridge = nodeInfoRegistry.createNode(1, "Bridge");
        Node loznice = nodeInfoRegistry.createNode(51, "Ložnice");
        Node pracovna = nodeInfoRegistry.createNode(44, "Pracovna");
        Node koupelna = nodeInfoRegistry.createNode(42, "Koupelna");
        Node pokoj = nodeInfoRegistry.createNode(43, "Pokoj");
        Node obyvak = nodeInfoRegistry.createNode(41, "Obývák");
        Node rele1 = nodeInfoRegistry.createNode(45, "Relé 1");
        Node rele2 = nodeInfoRegistry.createNode(46, "Relé 2");

        WallSwitch lozniceDvereSw = new WallSwitch("lozniceDvere", loznice, 1);
        WallSwitch loznicePostelSw = new WallSwitch("loznicePostel", loznice, 2);
        WallSwitch pracovnaSw = new WallSwitch("pracovna", pracovna, 1);
        WallSwitch koupelnaHorniSw = new WallSwitch("koupelnaHorni", koupelna, 1);
        WallSwitch koupelnaDolniSw = new WallSwitch("koupelnaDolni", koupelna, 2);
        WallSwitch wcSw = new WallSwitch("wc", koupelna, 3);
        WallSwitch pokojEditaSw = new WallSwitch("pokojEdita", pokoj, 1);
        WallSwitch pokojKluciSw = new WallSwitch("pokojKluci", pokoj, 2);
        WallSwitch obyvakSw1 = new WallSwitch("obyvak1", obyvak, 1);
        WallSwitch obyvakSw2 = new WallSwitch("obyvak2", obyvak, 2);
        WallSwitch obyvakSw3 = new WallSwitch("obyvak3", obyvak, 3);
        WallSwitch kuchynLinkaSw = new WallSwitch("kuchynLinka", rele2, 2);

        Relay16BoardDevice releBoard1 = new Relay16BoardDevice("rele1", rele1);
        Relay16BoardDevice releBoard2 = new Relay16BoardDevice("rele2", rele2, true, false, true);

        LouversController zaluzieKoupelna = addLouversController("lvKoupelna", "Koupelna", releBoard1.getRelay(1), releBoard1.getRelay(2), 60_000);
        LouversController zaluziePracovna = addLouversController("lvPracovna", "Pracovna", releBoard1.getRelay(3), releBoard1.getRelay(4), 60_000);
        LouversController zaluzieLoznice = addLouversController("lvLoznice", "Ložnice", releBoard1.getRelay(5), releBoard1.getRelay(6), 60_000);
        LouversController zaluziePokojEdita = addLouversController("lvPokojEdita", "Pokoj Edita", releBoard1.getRelay(7), releBoard1.getRelay(8), 60_000);
        LouversController zaluziePokojKluci = addLouversController("lvPokojKluci", "Pokoj kluci", releBoard1.getRelay(9), releBoard1.getRelay(10), 60_000);
        LouversController zaluzieTerasa1 = addLouversController("lvTerasa1", "Terasa 1", releBoard1.getRelay(11), releBoard1.getRelay(12), 60_000);
        LouversController zaluzieTerasa2 = addLouversController("lvTerasa2", "Terasa 2", releBoard1.getRelay(13), releBoard1.getRelay(14), 60_000);
        LouversController zaluzieJidelna = addLouversController("lvJidelna", "Jídelna", releBoard1.getRelay(15), releBoard1.getRelay(16), 60_000);
        LouversController zaluzie = addLouversController("lvKuchyn", "Kuchyň", releBoard2.getRelay(1), releBoard1.getRelay(2), 60_000);

        List<ServletAction> servletActions = new ArrayList<>();

        List<WebSocketHandler> wsHandlers = new ArrayList<>();
        // page handlers
        Page floorsPage = new StaticPage("/", "/floorPlan.html", "Mapa");
        List<Page> pages = new ArrayList<>();
        //noinspection CollectionAddAllCanBeReplacedWithConstructor
        pages.addAll(Arrays.asList(
                floorsPage,
                new LouversPage(louversControllers, pages),
                new NodeInfoPage(nodeInfoRegistry, pages, servletActions),
                new OptionsPage(OptionsSingleton.getInstance(), pages)));
        // rest handlers
        List<StatusHandler> deviceRestHandlers = Collections.singletonList(
                new LouversHandler(louversControllers));
//        configureSimulator(pages, wsHandlers, false);
        // rest/all handler
        List<Handler> handlers = new ArrayList<>();
        handlers.add(new NodeInfoDetailPage(nodeInfoRegistry, pages));
        handlers.addAll(pages);
        handlers.add(new StaticPage(VIRTUAL_CONFIGURATION_JS_FILENAME, "/configuration-ondra.js", null));
        handlers.add(new GetBackendUrlJs());
        handlers.add(new NodeHandler(nodeInfoRegistry));
        handlers.add(new ServletActionHandler(servletActions));
        handlers.addAll(deviceRestHandlers);
        handlers.add(new AllStatusHandler(deviceRestHandlers));
        servlet = new Servlet(handlers, floorsPage.getPath(), wsHandlers);

//        OnOffActor testLedActor = new OnOffActor("testLed", testOutputDevice3.getOut2(), 1, 0);
//        lst.addActionBinding(new ActionBinding(testInputDevice2.getIn1(), new Action[]{new SensorAction(testLedActor, 10)}, new Action[]{new SensorAction(testLedActor, 60)}));
    }

    @Override
    int getLouversMaxOffsetMs() {
        return 1600;
    }
}
