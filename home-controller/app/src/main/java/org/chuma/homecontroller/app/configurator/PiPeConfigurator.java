package org.chuma.homecontroller.app.configurator;

import java.util.ArrayList;

import org.chuma.homecontroller.app.servlet.Servlet;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.controller.ActionBinding;
import org.chuma.homecontroller.controller.action.Action;
import org.chuma.homecontroller.controller.action.SwitchOffAction;
import org.chuma.homecontroller.controller.action.SwitchOffSensorAction;
import org.chuma.homecontroller.controller.action.SwitchOnSensorAction;
import org.chuma.homecontroller.controller.actor.OnOffActor;
import org.chuma.homecontroller.controller.actor.PwmActor;
import org.chuma.homecontroller.controller.controller.LouversController;
import org.chuma.homecontroller.controller.controller.LouversControllerImpl;
import org.chuma.homecontroller.controller.controller.ValveController;
import org.chuma.homecontroller.controller.controller.ValveControllerImpl;
import org.chuma.homecontroller.controller.device.InputDevice;
import org.chuma.homecontroller.controller.device.LddBoardDevice;
import org.chuma.homecontroller.controller.device.RelayBoardDevice;
import org.chuma.homecontroller.controller.device.SwitchIndicator;
import org.chuma.homecontroller.controller.device.WallSwitch;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoCollector;
import org.chuma.homecontroller.controller.nodeinfo.SwitchListener;

public class PiPeConfigurator extends AbstractConfigurator {

    public PiPeConfigurator(NodeInfoCollector nodeInfoCollector) {
        super(nodeInfoCollector);
    }

    @Override
    public void configure() {
        SwitchListener lst = nodeInfoCollector.getSwitchListener();
        ArrayList<Action> lightsActions = new ArrayList<>();

        Node bridge = nodeInfoCollector.createNode(1, "Bridge");
        Node actor = nodeInfoCollector.createNode(44, "Actor");
        Node switches = nodeInfoCollector.createNode(43, "Switches");
        Node pirSensors = nodeInfoCollector.createNode(42, "PirSensors");

        WallSwitch switchASw = new WallSwitch("switchASw", switches, 1);
        WallSwitch switchBSw = new WallSwitch("switchBSw", switches, 2);
        WallSwitch switchCSw = new WallSwitch("switchCSw", switches, 3);

        // Rele51
        RelayBoardDevice rele51 = new RelayBoardDevice("rele51", actor, 1);

        // Zvonek
        OnOffActor zvonekActor = new OnOffActor("zvonek", "Zvonek", rele51.getRele4(), 0, 1,
                switchCSw.getRedLedIndicator(SwitchIndicator.Mode.SIGNAL_ALL_OFF), switchCSw.getGreenLedIndicator(SwitchIndicator.Mode.SIGNAL_ANY_ON));
        SwitchOnSensorAction zvonekAction = new SwitchOnSensorAction(zvonekActor, 5, 100);
        SwitchOffAction zvonekStopAction = new SwitchOffAction(zvonekActor);
        lst.addActionBinding(new ActionBinding(switchCSw.getRightUpperButton(), zvonekAction, null));
        lst.addActionBinding(new ActionBinding(switchCSw.getRightBottomButton(), zvonekStopAction, null));

        // Indikatory vratnice
        SwitchIndicator vratniceOffIndicator = new SwitchIndicator(switchASw.getRedLed(), SwitchIndicator.Mode.SIGNAL_ALL_OFF);
        SwitchIndicator vratniceOnIndicator = new SwitchIndicator(switchASw.getGreenLed(), SwitchIndicator.Mode.SIGNAL_ANY_ON);

        // LDD5
        LddBoardDevice lddDevice5 = new LddBoardDevice("lddDevice5", actor, 2, .35, .35, 1.0, 1.0, 1.0, 1.0);
        PwmActor vratnice1PwmActor = addLddLight(lightsActions, "pwmVrt1", "Vratnice 1", lddDevice5.getLdd1(), 0.35, vratniceOffIndicator, vratniceOnIndicator); // .6
        PwmActor vratnice2PwmActor = addLddLight(lightsActions, "pwmVrt2", "Vratnice 2", lddDevice5.getLdd2(), 0.35, vratniceOffIndicator, vratniceOnIndicator); // .72
        PwmActor led3PwmActor = addLddLight(lightsActions, "pwmZadH", "Zádveří", lddDevice5.getLdd3(), 0.35); // .36
        PwmActor led4PwmActor = addLddLight(lightsActions, "pwmKpHZrc", "Koupena zrcadla", lddDevice5.getLdd4(), 0.35); // .36
        PwmActor led5PwmActor = addLddLight(lightsActions, "pwmKpH", "Koupelna", lddDevice5.getLdd5(), 0.7); // .72
        PwmActor led6PwmActor = addLddLight(lightsActions, "pwmVchH", "Vchod hore", lddDevice5.getLdd6(), 1.0); // 1.08


        configurePwmLights(lst, switchASw, WallSwitch.Side.LEFT, 60, vratnice1PwmActor);
        configurePwmLights(lst, switchASw, WallSwitch.Side.RIGHT, 60, vratnice2PwmActor);


        // PIR
        InputDevice pirDevice = new InputDevice("pirDevice", pirSensors, 3);
        setupPir(lst, pirDevice.getIn1AndActivate(), "pirZadHVch", "Zadveri hore vchod",
                new SwitchOnSensorAction(vratnice2PwmActor, 600, 5), new SwitchOffSensorAction(vratnice2PwmActor, 10));

        // Louvers
        LouversController zaluzieVratnice;

        LouversController[] louversControllers = new LouversController[]{
                zaluzieVratnice = new LouversControllerImpl("lvVrt2", "Vratnice 2", rele51.getRele1(), rele51.getRele2(), 10000, 1000),
        };

        configureLouvers(lst, switchBSw, WallSwitch.Side.LEFT, zaluzieVratnice);

        // Air Valves
        ValveController vzduchVratnice;

        ValveController[] valveControllers = new ValveController[]{
                vzduchVratnice = new ValveControllerImpl("vlVrt", "Vratnice", rele51.getRele5(), rele51.getRele6(), 10000),
        };

//        Servlet.setLouversControllers(louversControllers);
//        Servlet.setValveControllers(valveControllers);
//        Servlet.setLightActions(lightsActions.toArray(new Action[lightsActions.size()]));
//        Servlet.pirStatusList = pirStatusList;
    }

    @Override
    public String getConfigurationJs() {
        return "configuration-pipe.js";
    }
}
