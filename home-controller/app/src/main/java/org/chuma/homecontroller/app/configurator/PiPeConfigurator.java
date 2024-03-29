package org.chuma.homecontroller.app.configurator;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.controller.ActionBinding;
import org.chuma.homecontroller.controller.action.ContinuousValueSwitchOnActionWithTimer;
import org.chuma.homecontroller.controller.action.SwitchOffAction;
import org.chuma.homecontroller.controller.action.SwitchOffActionWithTimer;
import org.chuma.homecontroller.controller.action.SwitchOnActionWithTimer;
import org.chuma.homecontroller.controller.actor.OnOffActor;
import org.chuma.homecontroller.controller.actor.PwmActor;
import org.chuma.homecontroller.controller.controller.LouversController;
import org.chuma.homecontroller.controller.controller.LouversControllerImpl;
import org.chuma.homecontroller.controller.controller.ValveController;
import org.chuma.homecontroller.controller.controller.ValveControllerImpl;
import org.chuma.homecontroller.controller.device.GenericInputDevice;
import org.chuma.homecontroller.controller.device.LddBoardDevice;
import org.chuma.homecontroller.controller.device.RelayBoardDevice;
import org.chuma.homecontroller.controller.device.SwitchIndicator;
import org.chuma.homecontroller.controller.device.WallSwitch;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoRegistry;
import org.chuma.homecontroller.controller.nodeinfo.NodeListener;
import org.chuma.homecontroller.controller.persistence.StateMap;

public class PiPeConfigurator extends AbstractConfigurator {

    public PiPeConfigurator(NodeInfoRegistry nodeInfoRegistry, StateMap stateMap) {
        super(nodeInfoRegistry, stateMap);
    }

    @Override
    public void configure() {
        NodeListener lst = nodeInfoRegistry.getNodeListener();

        Node bridge = nodeInfoRegistry.createNode(1, "Bridge");
        Node actor = nodeInfoRegistry.createNode(44, "Actor");
        Node switches = nodeInfoRegistry.createNode(43, "Switches");
        Node pirSensors = nodeInfoRegistry.createNode(42, "PirSensors");

        WallSwitch switchASw = new WallSwitch("switchASw", switches, 1);
        WallSwitch switchBSw = new WallSwitch("switchBSw", switches, 2);
        WallSwitch switchCSw = new WallSwitch("switchCSw", switches, 3);

        // Rele51
        RelayBoardDevice rele51 = new RelayBoardDevice("rele51", actor, 1);

        // Zvonek
        OnOffActor zvonekActor = new OnOffActor("zvonek", "Zvonek", rele51.getRelay4(),
                switchCSw.getRedLedIndicator(SwitchIndicator.Mode.SIGNAL_ALL_OFF), switchCSw.getGreenLedIndicator(SwitchIndicator.Mode.SIGNAL_ANY_ON));
        SwitchOnActionWithTimer zvonekAction = new SwitchOnActionWithTimer(zvonekActor, 5);
        SwitchOffAction zvonekStopAction = new SwitchOffAction(zvonekActor);
        lst.addActionBinding(new ActionBinding(switchCSw.getRightUpperButton(), zvonekAction, null));
        lst.addActionBinding(new ActionBinding(switchCSw.getRightBottomButton(), zvonekStopAction, null));

        // Indikatory vratnice
        SwitchIndicator vratniceOffIndicator = new SwitchIndicator(switchASw.getRedLed(), SwitchIndicator.Mode.SIGNAL_ALL_OFF);
        SwitchIndicator vratniceOnIndicator = new SwitchIndicator(switchASw.getGreenLed(), SwitchIndicator.Mode.SIGNAL_ANY_ON);

        // LDD5
        LddBoardDevice lddDevice5 = new LddBoardDevice("lddDevice5", actor, 2, .35, .35, 1.0, 1.0, 1.0, 1.0);
        PwmActor vratnice1PwmActor = addLddLight("pwmVrt1", "Vratnice 1", lddDevice5.getLdd1(), 0.35, vratniceOffIndicator, vratniceOnIndicator); // .6
        PwmActor vratnice2PwmActor = addLddLight("pwmVrt2", "Vratnice 2", lddDevice5.getLdd2(), 0.35, vratniceOffIndicator, vratniceOnIndicator); // .72
        PwmActor led3PwmActor = addLddLight("pwmZadH", "Zádveří", lddDevice5.getLdd3(), 0.35); // .36
        PwmActor led4PwmActor = addLddLight("pwmKpHZrc", "Koupena zrcadla", lddDevice5.getLdd4(), 0.35); // .36
        PwmActor led5PwmActor = addLddLight("pwmKpH", "Koupelna", lddDevice5.getLdd5(), 0.7); // .72
        PwmActor led6PwmActor = addLddLight("pwmVchH", "Vchod hore", lddDevice5.getLdd6(), 1.0); // 1.08


        configurePwmLights(switchASw, WallSwitch.Side.LEFT, 0.6, vratnice1PwmActor);
        configurePwmLights(switchASw, WallSwitch.Side.RIGHT, 0.6, vratnice2PwmActor);


        // PIR
        GenericInputDevice pirDevice = new GenericInputDevice("pirDevice", pirSensors, 3);
        setupPir(pirDevice.getIn1AndActivate(), "pirZadHVch", "Zadveri hore vchod",
                new ContinuousValueSwitchOnActionWithTimer(vratnice2PwmActor, 600, 0.05), new SwitchOffActionWithTimer(vratnice2PwmActor, 10));

        // Louvers
        LouversController zaluzieVratnice;

        LouversController[] louversControllers = new LouversController[]{
                zaluzieVratnice = new LouversControllerImpl("lvVrt2", "Vratnice 2", rele51.getRelay1(), rele51.getRelay2(), 10000, 1000, stateMap),
        };

        configureLouvers(switchBSw, WallSwitch.Side.LEFT, zaluzieVratnice);

        // Air Valves
        ValveController vzduchVratnice;

        ValveController[] valveControllers = new ValveController[]{
                vzduchVratnice = new ValveControllerImpl("vlVrt", "Vratnice", rele51.getRelay5(), rele51.getRelay6(),10000, stateMap),
        };

//        Servlet.setLouversControllers(louversControllers);
//        Servlet.setValveControllers(valveControllers);
//        Servlet.setLightActions(lightsActions.toArray(new Action[lightsActions.size()]));
//        Servlet.pirStatusList = pirStatusList;
    }

    @Override
    int getLouversMaxOffsetMs() {
        return 1600;
    }
}
