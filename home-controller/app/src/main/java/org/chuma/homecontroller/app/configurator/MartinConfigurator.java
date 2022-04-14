package org.chuma.homecontroller.app.configurator;

import java.util.ArrayList;
import java.util.List;

import org.chuma.homecontroller.app.servlet.Handler;
import org.chuma.homecontroller.app.servlet.Servlet;
import org.chuma.homecontroller.app.servlet.ServletAction;
import org.chuma.homecontroller.app.servlet.pages.NodeInfoPage;
import org.chuma.homecontroller.app.servlet.pages.Page;
import org.chuma.homecontroller.app.servlet.pages.StaticPage;
import org.chuma.homecontroller.app.servlet.pages.SystemPage;
import org.chuma.homecontroller.app.servlet.pages.TrainPage;
import org.chuma.homecontroller.app.servlet.pages.TrainWebSocketHandler;
import org.chuma.homecontroller.app.servlet.rest.NodeHandler;
import org.chuma.homecontroller.app.servlet.ws.WebSocketHandler;
import org.chuma.homecontroller.base.node.Node;
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

        new AbstractConnectedDevice("TrainControl", node37, 1, new String[] { "speed", "pass", "dir1", "-", "dir2", "-" }) {
            @Override
            public int getInitialOutputValues() {
                return 0;
            }

            @Override
            public int getEventMask() {
                return createMask(pins[1]);
            }

            @Override
            public int getOutputMasks() {
                return createMask(pins[0], pins[2], pins[4], /* added also remaining pins - we don't want them input */ pins[3], pins[5]);
            }
        };

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
        wsHandlers.add(new TrainWebSocketHandler(nodeInfoRegistry));
        List<ServletAction> rootActions = new ArrayList<>();
        List<Page> pages = new ArrayList<>();
        pages.add(new TrainPage(nodeInfoRegistry, pages));
        pages.add(new NodeInfoPage(nodeInfoRegistry, pages, rootActions));
        pages.add(new SystemPage(nodeInfoRegistry, pages));
        configureSimulator(pages, wsHandlers, true);
        List<Handler> handlers = new ArrayList<>(pages);
        handlers.add(new StaticPage(VIRTUAL_CONFIGURATION_JS_FILENAME, "/configuration-martin.js", null));
        handlers.add(new NodeHandler(nodeInfoRegistry));
        servlet = new Servlet(handlers, "/system", wsHandlers);
    }
}
