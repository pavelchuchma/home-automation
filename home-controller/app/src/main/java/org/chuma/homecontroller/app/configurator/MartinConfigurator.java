package org.chuma.homecontroller.app.configurator;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.event.Level;

import org.chuma.homecontroller.app.servlet.Handler;
import org.chuma.homecontroller.app.servlet.Servlet;
import org.chuma.homecontroller.app.servlet.ServletAction;
import org.chuma.homecontroller.app.servlet.pages.NodeInfoPage;
import org.chuma.homecontroller.app.servlet.pages.OptionsPage;
import org.chuma.homecontroller.app.servlet.pages.Page;
import org.chuma.homecontroller.app.servlet.pages.StaticPage;
import org.chuma.homecontroller.app.servlet.pages.GenericControlPage;
import org.chuma.homecontroller.app.servlet.pages.GenericControlWebSocketHandler;
import org.chuma.homecontroller.app.servlet.pages.GetBackendUrlJs;
import org.chuma.homecontroller.app.servlet.pages.LinkablePage;
import org.chuma.homecontroller.app.servlet.pages.NodeInfoDetailPage;
import org.chuma.homecontroller.app.servlet.rest.NodeHandler;
import org.chuma.homecontroller.app.servlet.rest.ServletActionHandler;
import org.chuma.homecontroller.app.servlet.ws.WebSocketHandler;
import org.chuma.homecontroller.app.train.TrainAutodrive;
import org.chuma.homecontroller.app.train.TrainControl;
import org.chuma.homecontroller.app.train.TrainPassSensor;
import org.chuma.homecontroller.app.train.TrainSwitch;
import org.chuma.homecontroller.app.train.TrainWebSocketHandler;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.NodePin;
import org.chuma.homecontroller.base.node.Pic;
import org.chuma.homecontroller.base.node.Pin;
import org.chuma.homecontroller.base.packet.simulation.SimulatedNode;
import org.chuma.homecontroller.base.packet.simulation.SimulatedNodeListener;
import org.chuma.homecontroller.base.packet.simulation.SimulatedPacketUartIO;
import org.chuma.homecontroller.controller.action.Action;
import org.chuma.homecontroller.controller.controller.LouversController;
import org.chuma.homecontroller.controller.controller.LouversControllerImpl;
import org.chuma.homecontroller.controller.device.AbstractConnectedDevice;
import org.chuma.homecontroller.controller.device.InputDevice;
import org.chuma.homecontroller.controller.device.OutputDevice;
import org.chuma.homecontroller.controller.device.RelayBoardDevice;
import org.chuma.homecontroller.controller.device.WallSwitch;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoRegistry;
import org.chuma.homecontroller.controller.nodeinfo.NodeListener;

import static org.chuma.homecontroller.app.servlet.pages.AbstractPage.VIRTUAL_CONFIGURATION_JS_FILENAME;

public class MartinConfigurator extends AbstractConfigurator {

    public MartinConfigurator(NodeInfoRegistry nodeInfoRegistry) {
        super(nodeInfoRegistry);
    }

    @Override
    public void configure() {
        NodeListener lst = nodeInfoRegistry.getNodeListener();
        ArrayList<Action> lightsActions = new ArrayList<>();

        Node bridge = nodeInfoRegistry.createNode(1, "Bridge");
//        Node node36 = nodeInfoRegistry.createNode(36, "Node36");
        Node node37 = nodeInfoRegistry.createNode(37, "Node37");
//        Node node40 = nodeInfoRegistry.createNode(40, "Node40");

//        WallSwitch switchASw = new WallSwitch("switchASw", node36, 1);
//        WallSwitch switchBSw = new WallSwitch("switchBSw", node36, 2);
        //WallSwitch switchCSw = new WallSwitch("switchCSw", node36, 3);

        // Rele51
//        RelayBoardDevice rele51 = new RelayBoardDevice("rele51", node37, 1);
        // Simple output device
//        OutputDevice outputOnly = new OutputDevice("OutChecker", node37, 1) {
//            public int getInitialOutputValues() { return 0; }
//        };
        if (false) {
        // Train device
        InputDevice inputOnly = new InputDevice("InputChecker", node37, 3);
        // Activate all pins
        inputOnly.getIn1AndActivate();
        inputOnly.getIn2AndActivate();
        inputOnly.getIn3AndActivate();
        inputOnly.getIn4AndActivate();
        inputOnly.getIn5AndActivate();
        inputOnly.getIn6AndActivate();
        }

        OutputDevice out = new OutputDevice("Power", node37, 1, new String[] { "speedA", "speedB", "dirALeft", "dirBLeft", "dirARight", "dirBRight" }) {
            public int getInitialOutputValues() { return 0; }
        };
        RelayBoardDevice tsr = new RelayBoardDevice("TrainSwitchRelay", node37, 2);
        InputDevice in = new InputDevice("Status", node37, 3, new String[] { "swStr", "passA", "swTurn", "passB", "n/a", "passC" });
        NodePin passA = in.getIn2AndActivate();
        NodePin passB = in.getIn4AndActivate();
        NodePin passC = in.getIn6AndActivate();

        // TODO: Configure to different location on PI? Where? Probably via instanceof SimulatedPacketUartIO
        Options options = new Options("/tmp/train.properties", "train.properties");

        TrainSwitch vyhybka = new TrainSwitch("Vyhybka", lst, options, tsr.getRelay6(), tsr.getRelay5(), in.getIn1AndActivate(), in.getIn3AndActivate());
        TrainControl vlak = new TrainControl("vlak", options, out.getOut3(), out.getOut5(), out.getOut1());
        TrainPassSensor sensorA = new TrainPassSensor("sensor-A", lst, options, passA).withTrainPosition(vlak);
        TrainPassSensor sensorB = new TrainPassSensor("sensor-B", lst, options, passB).withTrainPosition(vlak);
        TrainPassSensor sensorC = new TrainPassSensor("sensor-C", lst, options, passC).withTrainPosition(vlak);
        TrainAutodrive autodrive = new TrainAutodrive("autodrive", options, vlak, vyhybka, sensorA, sensorB, sensorC);
        
        // (vyhybky)
//        LouversController vyhybka01;
//        LouversController vyhybka02;
//        LouversController vyhybka03;
//
//        LouversController[] louversControllers = new LouversController[]{
//                vyhybka01 = new LouversControllerImpl("lvVyh01", "Vyhybka 01", rele51.getRelay1(), rele51.getRelay2(), 1000, 10),
//                vyhybka02 = new LouversControllerImpl("lvVyh02", "Vyhybka 02", rele51.getRelay3(), rele51.getRelay4(), 1000, 10),
//                vyhybka03 = new LouversControllerImpl("lvVyh03", "Vyhybka 03", rele51.getRelay5(), rele51.getRelay6(), 1000, 10),
//        };



//        configureLouvers(lst, switchASw, WallSwitch.Side.LEFT, vyhybka01);
//        configureLouvers(lst, switchASw, WallSwitch.Side.RIGHT, vyhybka02);
//        configureLouvers(lst, switchBSw, WallSwitch.Side.LEFT, vyhybka03);

        List<WebSocketHandler> wsHandlers = new ArrayList<>();
//        wsHandlers.add(new GenericControlWebSocketHandler(nodeInfoRegistry));
        wsHandlers.add(new TrainWebSocketHandler(autodrive, vyhybka, vlak, sensorA, sensorB, sensorC));
        List<ServletAction> servletActions = new ArrayList<>();
        Page trainPage = new StaticPage("/", "/train.html", "Train");
        List<Page> pages = new ArrayList<>();
        pages.add(trainPage);
//      pages.add(new GenericControlPage(nodeInfoRegistry, pages));
        pages.add(new NodeInfoPage(nodeInfoRegistry, pages, servletActions));
        pages.add(new OptionsPage(options, pages));
        configureSimulator(pages, wsHandlers, true);
        List<Handler> handlers = new ArrayList<>();
        handlers.add(new NodeInfoDetailPage(nodeInfoRegistry, pages));
        handlers.addAll(pages);
        handlers.add(new StaticPage(VIRTUAL_CONFIGURATION_JS_FILENAME, "/configuration-martin.js", null));
        handlers.add(new GetBackendUrlJs());
        handlers.add(new NodeHandler(nodeInfoRegistry));
        handlers.add(new ServletActionHandler(servletActions));
        servlet = new Servlet(handlers, trainPage.getPath(), wsHandlers);

        if (nodeInfoRegistry.getPacketUartIO() instanceof SimulatedPacketUartIO) {
            // Simulation - set initial state of train switch (one direction must be set)
            SimulatedPacketUartIO sim = (SimulatedPacketUartIO)nodeInfoRegistry.getPacketUartIO();
            SimulatedNode n = sim.getSimulatedNode(node37.getNodeId());
            // We set turn pin to 1 and straight leave in 0 => switch is set to straight
            n.initializePin(vyhybka.getIndicatorTurnPin().getPin(), 1);
            // We set train pass sensors to 1 - inactive (no train)
            n.initializePin(passA.getPin(), 1);
            n.initializePin(passB.getPin(), 1);
            n.initializePin(passC.getPin(), 1);

            sim.addListener(new SimulatedNodeListener() {
                @Override
                public void onSetTris(SimulatedNode node, int port, int value) {
                }
                
                @Override
                public void onSetPort(SimulatedNode node, int port, int value) {
                    // When switch is in direction, indicator pin is 0 == down
                    handleSwitchPin(node, port, value, vyhybka.getSwitchTurnPin(), vyhybka.getIndicatorStraightPin(), vyhybka.getIndicatorTurnPin());
                    handleSwitchPin(node, port, value, vyhybka.getSwitchStraightPin(), vyhybka.getIndicatorTurnPin(), vyhybka.getIndicatorStraightPin());
                }
                
                private void handleSwitchPin(SimulatedNode node, int port, int value, NodePin onPin, NodePin up, NodePin down) {
                    if (node.getId() == onPin.getNode().getNodeId()) {
                        // Simulate train switch behavior
                        Pin p = onPin.getPin();
                        if (port == p.getPortIndex() && (value & p.getBitMask()) != 0) {
                            // Out pin is set - change state of signal pins
                            try {
                                sim.getSimulatedNode(down.getNode().getNodeId()).setInputPin(down.getPin(), 0);
                                sim.getSimulatedNode(up.getNode().getNodeId()).setInputPin(up.getPin(), 1);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                @Override
                public void onSetManualPwm(SimulatedNode node, int port, int pin, int value) {
                }
                
                @Override
                public void onSetEventMask(SimulatedNode node, int port, int mask) {
                }
                
                @Override
                public void logMessage(SimulatedNode node, Level level, String messageFormat, Object... args) {
                }
            });
        }


        // TODO:
        // - manual control - speed, direction, switch
        // - indicators - train above sensor, switch position
        // - auto - with stop
    }
}
