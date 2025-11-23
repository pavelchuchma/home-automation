package org.chuma.homecontroller.app.configurator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.chuma.homecontroller.app.servlet.rest.AirValveHandler;
import org.chuma.homecontroller.app.servlet.rest.AllStatusHandler;
import org.chuma.homecontroller.app.servlet.rest.BoilerHandler;
import org.chuma.homecontroller.app.servlet.rest.ElectricitySpotPriceHandler;
import org.chuma.homecontroller.app.servlet.rest.FuturaHandler;
import org.chuma.homecontroller.app.servlet.rest.HvacHandler;
import org.chuma.homecontroller.app.servlet.rest.InverterHandler;
import org.chuma.homecontroller.app.servlet.rest.LouversHandler;
import org.chuma.homecontroller.app.servlet.rest.NodeHandler;
import org.chuma.homecontroller.app.servlet.rest.OnOffHandler;
import org.chuma.homecontroller.app.servlet.rest.PirHandler;
import org.chuma.homecontroller.app.servlet.rest.PwmLightsHandler;
import org.chuma.homecontroller.app.servlet.rest.RobonectHandler;
import org.chuma.homecontroller.app.servlet.rest.ServletActionHandler;
import org.chuma.homecontroller.app.servlet.rest.StatusHandler;
import org.chuma.homecontroller.app.servlet.rest.WaterPumpHandler;
import org.chuma.homecontroller.app.servlet.ws.WebSocketHandler;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.utils.Options;
import org.chuma.homecontroller.base.utils.OptionsSingleton;
import org.chuma.homecontroller.controller.ActionBinding;
import org.chuma.homecontroller.controller.action.Action;
import org.chuma.homecontroller.controller.action.ContinuousValueSwitchOnActionWithTimer;
import org.chuma.homecontroller.controller.action.GenericCodeAction;
import org.chuma.homecontroller.controller.action.IndicatorAction;
import org.chuma.homecontroller.controller.action.InvertAction;
import org.chuma.homecontroller.controller.action.InvertActionWithTimer;
import org.chuma.homecontroller.controller.action.Relay16TestLoopAction;
import org.chuma.homecontroller.controller.action.SwitchAllOffWithMemory;
import org.chuma.homecontroller.controller.action.SwitchOffAction;
import org.chuma.homecontroller.controller.action.SwitchOffActionWithTimer;
import org.chuma.homecontroller.controller.action.SwitchOnAction;
import org.chuma.homecontroller.controller.action.SwitchOnActionWithTimer;
import org.chuma.homecontroller.controller.action.condition.DarkCondition;
import org.chuma.homecontroller.controller.action.condition.ICondition;
import org.chuma.homecontroller.controller.action.condition.PressDurationCondition;
import org.chuma.homecontroller.controller.actor.ActorListener;
import org.chuma.homecontroller.controller.actor.IOnOffActor;
import org.chuma.homecontroller.controller.actor.LddActor;
import org.chuma.homecontroller.controller.actor.PwmActor;
import org.chuma.homecontroller.controller.actor.VoidOnOffActor;
import org.chuma.homecontroller.controller.controller.LouversController;
import org.chuma.homecontroller.controller.controller.ValveController;
import org.chuma.homecontroller.controller.controller.ValveControllerImpl;
import org.chuma.homecontroller.controller.device.GenericInputDevice;
import org.chuma.homecontroller.controller.device.GenericOutputDevice;
import org.chuma.homecontroller.controller.device.LddBoardDevice;
import org.chuma.homecontroller.controller.device.Relay16BoardDevice;
import org.chuma.homecontroller.controller.device.RelayBoardDevice;
import org.chuma.homecontroller.controller.device.SwitchIndicator;
import org.chuma.homecontroller.controller.device.TriacBoardDevice;
import org.chuma.homecontroller.controller.device.WallSwitch;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoRegistry;
import org.chuma.homecontroller.controller.persistence.StateMap;
import org.chuma.homecontroller.extensions.action.condition.SunCondition;
import org.chuma.homecontroller.extensions.actor.HvacActor;
import org.chuma.homecontroller.extensions.actor.RadioOnOffActor;
import org.chuma.homecontroller.extensions.actor.WaterPumpMonitor;
import org.chuma.homecontroller.extensions.external.SunCalculator;
import org.chuma.homecontroller.extensions.external.boiler.BoilerController;
import org.chuma.homecontroller.extensions.external.boiler.BoilerManager;
import org.chuma.homecontroller.extensions.external.boiler.BoilerMonitor;
import org.chuma.homecontroller.extensions.external.futura.FuturaMonitor;
import org.chuma.homecontroller.extensions.external.garage.GarageManager;
import org.chuma.homecontroller.extensions.external.inverter.ElectricitySpotPriceMonitor;
import org.chuma.homecontroller.extensions.external.inverter.InverterManager;
import org.chuma.homecontroller.extensions.external.inverter.InverterMonitor;
import org.chuma.homecontroller.extensions.external.inverter.impl.SolaxInverterModbusClient;
import org.chuma.homecontroller.extensions.external.inverter.impl.SolaxInverterMonitor;
import org.chuma.homecontroller.extensions.external.robonect.RobonectMonitor;
import org.chuma.homecontroller.extensions.external.robonect.client.RobonectClient;
import org.chuma.homecontroller.extensions.external.robonect.client.RobonectEndpoint;
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
        Node relay16testNode46 = nodeInfoRegistry.createNode(46, "Relay16testNode46");
        Node relay16testNode47 = nodeInfoRegistry.createNode(47, "Relay16testNode48");
        Node relay16testNode48 = nodeInfoRegistry.createNode(48, "Relay16testNode47");


        InverterManager inverterManager = null;
        SolaxInverterMonitor inverterMonitor = null;
        try {
            SolaxInverterModbusClient inverterModbusClient = new SolaxInverterModbusClient(OptionsSingleton.get("inverter.ip"));
            inverterMonitor = new SolaxInverterMonitor(inverterModbusClient, 5_000, 60_000);
            inverterMonitor.start();

            inverterManager = configureInverterRemoteControl(inverterModbusClient, inverterMonitor);
        } catch (Exception e) {
            log.error("Failed to init solax inverter client", e);
        }

        ElectricitySpotPriceMonitor priceMonitor = new ElectricitySpotPriceMonitor(
                OptionsSingleton.getDouble("electricity.price.distribution-fee"),
                OptionsSingleton.getDouble("electricity.price.sell-fee"),
                OptionsSingleton.getDouble("electricity.price.vat")
        );


        List<ServletAction> servletActions = new ArrayList<>();

        //test wall switch application
//        WallSwitch testSw = new WallSwitch("testSwA", switchTestNode, 1, 0.01);
        servletActions.add(new ServletAction("testRele16-46", "Rele16-46", new Relay16TestLoopAction(new Relay16BoardDevice("test46", relay16testNode46))));
        servletActions.add(new ServletAction("testRele16-47", "Rele16-47", new Relay16TestLoopAction(new Relay16BoardDevice("test47", relay16testNode47))));
        servletActions.add(new ServletAction("testRele16-48", "Rele16-48", new Relay16TestLoopAction(new Relay16BoardDevice("test48", relay16testNode48))));

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

    private static InverterManager configureInverterRemoteControl(SolaxInverterModbusClient client, InverterMonitor inverterMonitor) {
        final Options options = OptionsSingleton.getInstance();
        return new InverterManager(client, options);
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
