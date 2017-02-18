package app.configurator;

import java.util.ArrayList;

import app.NodeInfoCollector;
import app.SwitchListener;
import controller.action.Action;
import controller.actor.OnOffActor;
import controller.actor.PwmActor;
import controller.controller.LouversController;
import controller.controller.LouversControllerImpl;
import controller.controller.ValveController;
import controller.controller.ValveControllerImpl;
import controller.device.LddBoardDevice;
import controller.device.RelayBoardDevice;
import controller.device.SwitchIndicator;
import controller.device.WallSwitch;
import node.Node;
import servlet.Servlet;

public class PiPeConfigurator extends AbstractConfigurator {

    public PiPeConfigurator(NodeInfoCollector nodeInfoCollector) {
        super(nodeInfoCollector);
    }

    @Override
    public void configure() {

        Node bridge = nodeInfoCollector.createNode(1, "Bridge");
        Node actor = nodeInfoCollector.createNode(44, "Actor");
        Node switches = nodeInfoCollector.createNode(3, "Switches");

        WallSwitch switchASw = new WallSwitch("chodbaALSw", switches, 1);
        WallSwitch switchBSw = new WallSwitch("switchBSw", switches, 2);
        WallSwitch switchCSw = new WallSwitch("switchCSw", switches, 3);

        // Rele51
        RelayBoardDevice rele51 = new RelayBoardDevice("rele51", actor, 1);

        OnOffActor zvonekActor = new OnOffActor("zvonek", "Zvonek", rele51.getRele4(), 1, 0,
                switchCSw.getRedLedIndicator(SwitchIndicator.Mode.SIGNAL_ALL_OFF), switchCSw.getGreenLedIndicator(SwitchIndicator.Mode.SIGNAL_ALL_OFF));

        ArrayList<Action> lightsActions = new ArrayList<>();

        // LDD5
        LddBoardDevice lddDevice5 = new LddBoardDevice("lddDevice5", actor, 2, .35, .35, 1.0, 1.0, 1.0, 1.0);
        PwmActor led1PwmActor = addLddLight(lightsActions, "pwmLed1", "Led 1", lddDevice5.getLdd1(), 0.35); // .36
        PwmActor led2PwmActor = addLddLight(lightsActions, "pwmLed2", "Led 2", lddDevice5.getLdd2(), 0.35); // .36
        PwmActor vratnice1PwmActor = addLddLight(lightsActions, "pwmVrt1", "Vratnice 1", lddDevice5.getLdd3(), 0.96); // .6
        PwmActor vratnice2PwmActor = addLddLight(lightsActions, "pwmVrt2", "Vratnice 2", lddDevice5.getLdd4(), 1.0); // .72
        PwmActor led5PwmActor = addLddLight(lightsActions, "pwmLed5", "Led 5", lddDevice5.getLdd5(), 0.7); // .72
        PwmActor led6PwmActor = addLddLight(lightsActions, "pwmLed6", "Led 6", lddDevice5.getLdd6(), 1.0); // 1.08


        SwitchListener lst = nodeInfoCollector.getSwitchListener();


        LouversController zaluzieVratnice;

        int snowConstant = 3000;
        LouversController[] louversControllers = new LouversController[]{
                zaluzieVratnice = new LouversControllerImpl("lvVrt2", "Vratnice 2", rele51.getRele1(), rele51.getRele2(), 5000, 1600),
        };

        ValveController vzduchVratnice;

        ValveController[] valveControllers = new ValveController[]{
                vzduchVratnice = new ValveControllerImpl("vlVrt", "Vratnice", rele51.getRele5(), rele51.getRele6(), 10000),
        };

        Servlet.setLouversControllers(louversControllers);
        Servlet.setValveControllers(valveControllers);
        Servlet.setLightActions(lightsActions.toArray(new Action[lightsActions.size()]));
        Servlet.pirStatusList = pirStatusList;
    }

    @Override
    public String getConfigurationJs() {
        return "configuration-pipe.js";
    }
}
