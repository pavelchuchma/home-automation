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
import org.chuma.homecontroller.controller.action.Action;
import org.chuma.homecontroller.controller.actor.Actor;
import org.chuma.homecontroller.controller.controller.LouversController;
import org.chuma.homecontroller.controller.device.Relay16BoardDevice;
import org.chuma.homecontroller.controller.device.WallSwitch;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoRegistry;
import org.chuma.homecontroller.controller.persistence.StateMap;

@SuppressWarnings({"unused", "DuplicatedCode", "SpellCheckingInspection"})
public class OndraConfigurator extends AbstractConfigurator {

    private static final String CFG_INVERTER_MANAGER_HIGH_TARIFF_BATTERY_RESERVE = "inverter.manager.high.tariff.battery.reserve";
    private static final String CFG_INVERTER_MANAGER_HIGH_TARIFF_TIMES = "inverter.manager.high.tariff.times";
    private static final String CFG_INVERTER_MANAGER_MINIMAL_SOC = "inverter.manager.minimal.soc";
    static Logger log = LoggerFactory.getLogger(OndraConfigurator.class.getName());

    public OndraConfigurator(NodeInfoRegistry nodeInfoRegistry, StateMap stateMap) {
        super(nodeInfoRegistry, stateMap);
    }

    private static class NgrokAction implements Action {
        private final int port;

        public NgrokAction(int port) {
            this.port = port;
        }

        @Override
        public void perform(int timeSinceLastAction) {
            try {
                Runtime.getRuntime().exec("/usr/local/bin/ngrok tcp " + port);
            } catch (IOException e) {
                log.error("Failed to start ngrok process", e);
            }
        }

        @Override
        public Actor getActor() {
            return null;
        }
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

        LouversController zaluzieKoupelna = addLouversController("lvKoupelna", "Koupelna", releBoard1.getRelay(1), releBoard1.getRelay(2), 38_000);
        LouversController zaluziePracovna = addLouversController("lvPracovna", "Pracovna", releBoard1.getRelay(3), releBoard1.getRelay(4), 48_000);
        LouversController zaluzieLoznice = addLouversController("lvLoznice", "Ložnice", releBoard1.getRelay(5), releBoard1.getRelay(6), 48_000);
        LouversController zaluziePokojKluci = addLouversController("lvPokojKluci", "Pokoj kluci", releBoard1.getRelay(7), releBoard1.getRelay(8), 48_000);
        LouversController zaluziePokojEdita = addLouversController("lvPokojEdita", "Pokoj Edita", releBoard1.getRelay(9), releBoard1.getRelay(10), 48_000);
        LouversController zaluzieTerasa1 = addLouversController("lvTerasa1", "Terasa 1", releBoard1.getRelay(11), releBoard1.getRelay(12), 67_000);
        LouversController zaluzieTerasa2 = addLouversController("lvTerasa2", "Terasa 2", releBoard2.getRelay(1), releBoard2.getRelay(2), 67_000);
        LouversController zaluzieKuchyn = addLouversController("lvKuchyn", "Kuchyň", releBoard1.getRelay(13), releBoard1.getRelay(14), 38_000);
        LouversController zaluzieJidelna = addLouversController("lvJidelna", "Jídelna", releBoard1.getRelay(15), releBoard1.getRelay(16), 48_000);

        configureLouvers(lozniceDvereSw, WallSwitch.Side.LEFT, zaluzieLoznice);
        configureLouvers(lozniceDvereSw, WallSwitch.Side.RIGHT, zaluzieLoznice);

//        configureLouvers(loznicePostelSw, WallSwitch.Side.LEFT, zaluzieKoupelna, zaluziePracovna, zaluzieLoznice,
//                zaluziePokojEdita, zaluziePokojKluci, zaluzieTerasa1, zaluzieTerasa2, zaluzieJidelna, zaluzieKuchyn);
        configureLouvers(loznicePostelSw, WallSwitch.Side.RIGHT, zaluzieLoznice);

        configureLouvers(pracovnaSw, WallSwitch.Side.LEFT, zaluziePracovna);
        configureLouvers(pracovnaSw, WallSwitch.Side.RIGHT, zaluziePracovna);

        configureLouvers(pokojEditaSw, WallSwitch.Side.LEFT, zaluziePokojKluci);
        configureLouvers(pokojEditaSw, WallSwitch.Side.RIGHT, zaluziePokojEdita);
        configureLouvers(pokojKluciSw, WallSwitch.Side.LEFT, zaluziePokojKluci);
        configureLouvers(pokojKluciSw, WallSwitch.Side.RIGHT, zaluziePokojEdita);

        configureLouvers(koupelnaHorniSw, WallSwitch.Side.LEFT, zaluzieKoupelna);
        configureLouvers(koupelnaHorniSw, WallSwitch.Side.RIGHT, zaluzieKoupelna);

        configureLouvers(obyvakSw1, WallSwitch.Side.LEFT, zaluzieTerasa1);
        configureLouvers(obyvakSw1, WallSwitch.Side.RIGHT, zaluzieTerasa2);
        configureLouvers(obyvakSw2, WallSwitch.Side.LEFT, zaluzieJidelna);
        configureLouvers(obyvakSw2, WallSwitch.Side.RIGHT, zaluzieKuchyn);
        configureLouvers(obyvakSw3, WallSwitch.Side.LEFT, zaluzieTerasa1, zaluzieTerasa2, zaluzieJidelna, zaluzieKuchyn);
//        configureLouvers(obyvakSw3, WallSwitch.Side.RIGHT, zaluzieKoupelna, zaluziePracovna, zaluzieLoznice,
//                zaluziePokojEdita, zaluziePokojKluci, zaluzieTerasa1, zaluzieTerasa2, zaluzieJidelna, zaluzieKuchyn);


        List<ServletAction> servletActions = new ArrayList<>();
        servletActions.add(new ServletAction("ngrokSsh", "Start ngrok SSH", new NgrokAction(22)));
        servletActions.add(new ServletAction("ngrokHttp", "Start ngrok HTTP", new NgrokAction(80)));
        servletActions.add(new ServletAction("ngrokKodiWeb", "Start ngrok KodiWeb", new NgrokAction(8080)));

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
