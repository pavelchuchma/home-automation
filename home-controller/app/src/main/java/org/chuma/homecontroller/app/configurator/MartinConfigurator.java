package org.chuma.homecontroller.app.configurator;

import java.util.ArrayList;

import org.chuma.homecontroller.app.servlet.Servlet;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.controller.action.Action;
import org.chuma.homecontroller.controller.controller.LouversController;
import org.chuma.homecontroller.controller.controller.LouversControllerImpl;
import org.chuma.homecontroller.controller.controller.ValveController;
import org.chuma.homecontroller.controller.device.RelayBoardDevice;
import org.chuma.homecontroller.controller.device.WallSwitch;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoCollector;
import org.chuma.homecontroller.controller.nodeinfo.SwitchListener;

public class MartinConfigurator extends AbstractConfigurator {

    public MartinConfigurator(NodeInfoCollector nodeInfoCollector) {
        super(nodeInfoCollector);
    }

    @Override
    public void configure() {
        SwitchListener lst = nodeInfoCollector.getSwitchListener();
        ArrayList<Action> lightsActions = new ArrayList<>();

        Node bridge = nodeInfoCollector.createNode(1, "Bridge");
        Node node36 = nodeInfoCollector.createNode(36, "Node36");
        Node node37 = nodeInfoCollector.createNode(37, "Node37");
        Node node40 = nodeInfoCollector.createNode(40, "Node40");

        WallSwitch switchASw = new WallSwitch("switchASw", node36, 1);
        WallSwitch switchBSw = new WallSwitch("switchBSw", node36, 2);
        //WallSwitch switchCSw = new WallSwitch("switchCSw", node36, 3);

        // Rele51
        RelayBoardDevice rele51 = new RelayBoardDevice("rele51", node37, 1);

        // (vyhybky)
        LouversController vyhybka01;
        LouversController vyhybka02;
        LouversController vyhybka03;

        LouversController[] louversControllers = new LouversController[]{
                vyhybka01 = new LouversControllerImpl("lvVyh01", "Vyhybka 01", rele51.getRele1(), rele51.getRele2(), 1000, 10),
                vyhybka02 = new LouversControllerImpl("lvVyh02", "Vyhybka 02", rele51.getRele3(), rele51.getRele4(), 1000, 10),
                vyhybka03 = new LouversControllerImpl("lvVyh03", "Vyhybka 03", rele51.getRele5(), rele51.getRele6(), 1000, 10),
        };


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
    public String getConfigurationJs() {
        return "configuration-martin.js";
    }
}
