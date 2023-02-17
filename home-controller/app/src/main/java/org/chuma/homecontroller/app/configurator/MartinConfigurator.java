package org.chuma.homecontroller.app.configurator;

import java.util.ArrayList;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.controller.action.Action;
import org.chuma.homecontroller.controller.controller.LouversController;
import org.chuma.homecontroller.controller.controller.ValveController;
import org.chuma.homecontroller.controller.device.RelayBoardDevice;
import org.chuma.homecontroller.controller.device.WallSwitch;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoRegistry;
import org.chuma.homecontroller.controller.nodeinfo.NodeListener;

public class MartinConfigurator extends AbstractConfigurator {

    public MartinConfigurator(NodeInfoRegistry nodeInfoRegistry) {
        super(nodeInfoRegistry);
    }

    @Override
    public void configure() {
        NodeListener lst = nodeInfoRegistry.getNodeListener();
        ArrayList<Action> lightsActions = new ArrayList<>();

        Node bridge = nodeInfoRegistry.createNode(1, "Bridge");
        Node node36 = nodeInfoRegistry.createNode(36, "Node36");
        Node node37 = nodeInfoRegistry.createNode(37, "Node37");
        Node node40 = nodeInfoRegistry.createNode(40, "Node40");

        WallSwitch switchASw = new WallSwitch("switchASw", node36, 1);
        WallSwitch switchBSw = new WallSwitch("switchBSw", node36, 2);
        //WallSwitch switchCSw = new WallSwitch("switchCSw", node36, 3);

        // Rele51
        RelayBoardDevice rele51 = new RelayBoardDevice("rele51", node37, 1);

        // (vyhybky)
        LouversController vyhybka01 = addLouversController("lvVyh01", "Vyhybka 01", rele51.getRelay1(), rele51.getRelay2(), 1000);
        LouversController vyhybka02 = addLouversController("lvVyh02", "Vyhybka 02", rele51.getRelay3(), rele51.getRelay4(), 1000);
        LouversController vyhybka03 = addLouversController("lvVyh03", "Vyhybka 03", rele51.getRelay5(), rele51.getRelay6(), 1000);

        configureLouvers(lst, switchASw, WallSwitch.Side.LEFT, vyhybka01);
        configureLouvers(lst, switchASw, WallSwitch.Side.RIGHT, vyhybka02);
        configureLouvers(lst, switchBSw, WallSwitch.Side.LEFT, vyhybka03);


        // Air Valves
//        ValveController vzduchVratnice;

        ValveController[] valveControllers = new ValveController[]{
//                vzduchVratnice = new ValveControllerImpl("vlVrt", "Vratnice", rele51.getRele5(), rele51.getRele6(), 10000),
        };

//        Servlet.setLouversControllers(louversControllers);
//        Servlet.setValveControllers(valveControllers);
//        Servlet.setLightActions(lightsActions.toArray(new Action[lightsActions.size()]));
//        Servlet.pirStatusList = pirStatusList;
    }

    @Override
    int getLouversMaxOffsetMs() {
        return 10;
    }
}
