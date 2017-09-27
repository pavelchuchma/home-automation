package app.configurator;

import java.util.ArrayList;
import java.util.List;

import app.NodeInfoCollector;
import app.SwitchListener;
import controller.ActionBinding;
import controller.PirStatus;
import controller.action.Action;
import controller.action.DecreasePwmAction;
import controller.action.IncreasePwmAction;
import controller.action.LouversActionGroup;
import controller.action.PwmActionGroup;
import controller.action.SwitchOffAction;
import controller.action.SwitchOnAction;
import controller.actor.ActorListener;
import controller.actor.IOnOffActor;
import controller.actor.PwmActor;
import controller.controller.LouversController;
import controller.device.LddBoardDevice;
import controller.device.SwitchIndicator;
import controller.device.WallSwitch;
import node.NodePin;
import servlet.Servlet;

public abstract class AbstractConfigurator {
    protected NodeInfoCollector nodeInfoCollector;
    protected List<PirStatus> pirStatusList = new ArrayList<>();

    public AbstractConfigurator(NodeInfoCollector nodeInfoCollector) {
        this.nodeInfoCollector = nodeInfoCollector;
        Servlet.configurator = this;
    }

    protected static void configurePwmLights(SwitchListener lst, WallSwitch wallSwitch, WallSwitch.Side side, int initialPwmValue, PwmActor... pwmActors) {
        configurePwmLightsImpl(lst, wallSwitch, side, initialPwmValue, pwmActors, null);
    }

    protected static void configurePwmLightsImpl(SwitchListener lst, WallSwitch wallSwitch, WallSwitch.Side side, int initialPwmValue, PwmActor[] pwmActors, IOnOffActor[] switchOffOnlyActors) {
        List<Action> upperButtonUpActions = new ArrayList<>();
        List<Action> upperButtonDownActions = new ArrayList<>();
        List<Action> downButtonUpActions = new ArrayList<>();
        List<Action> downButtonDownActions = new ArrayList<>();

        for (PwmActor pwmActor : pwmActors) {
            PwmActionGroup actionGroup = new PwmActionGroup(pwmActor, initialPwmValue);
            upperButtonUpActions.add(actionGroup.getUpButtonDownAction());
            upperButtonDownActions.add(actionGroup.getUpButtonUpAction());
            downButtonUpActions.add(actionGroup.getDownButtonDownAction());
            downButtonDownActions.add(actionGroup.getDownButtonUpAction());
        }
        if (switchOffOnlyActors != null) {
            for (IOnOffActor pwmActor : switchOffOnlyActors) {
                downButtonDownActions.add(new SwitchOffAction(pwmActor));
            }
        }

        NodePin upperButton = getUpperButton(wallSwitch, side);
        NodePin bottomButton = getBottomButton(wallSwitch, side);
        lst.addActionBinding(new ActionBinding(upperButton, toArray(upperButtonUpActions), toArray(upperButtonDownActions)));
        lst.addActionBinding(new ActionBinding(bottomButton, toArray(downButtonUpActions), toArray(downButtonDownActions)));
    }

    private static Action[] toArray(List<Action> list) {
        return list.toArray(new Action[list.size()]);
    }

    static PwmActor addLddLight(ArrayList<Action> lightsActions, String id, String label, LddBoardDevice.LddNodePin pin, double maxLoad, ActorListener... actorListeners) {
        PwmActor pwmActor = new PwmActor(id, label, pin, maxLoad / pin.getMaxLddCurrent(), actorListeners);
        lightsActions.add(new SwitchOnAction(pwmActor));
        lightsActions.add(new IncreasePwmAction(pwmActor));
        lightsActions.add(new DecreasePwmAction(pwmActor));
        lightsActions.add(new SwitchOffAction(pwmActor));
        return pwmActor;
    }

    static void configureLouvers(SwitchListener lst, WallSwitch wallSwitch, WallSwitch.Side side, LouversController... louversControllers) {
        Action[] upButtonDownAction = new Action[louversControllers.length];
        Action[] upButtonUpAction = new Action[louversControllers.length];
        Action[] downButtonDownAction = new Action[louversControllers.length];
        Action[] downButtonUpAction = new Action[louversControllers.length];
        for (int i = 0; i < louversControllers.length; i++) {
            LouversActionGroup group = new LouversActionGroup(louversControllers[i], wallSwitch.getGreenLedIndicator(SwitchIndicator.Mode.SIGNAL_ANY_ON));
            upButtonDownAction[i] = group.getUpButtonDownAction();
            upButtonUpAction[i] = group.getUpButtonUpAction();
            downButtonDownAction[i] = group.getDownButtonDownAction();
            downButtonUpAction[i] = group.getDownButtonUpAction();
        }

        NodePin upTrigger = getUpperButton(wallSwitch, side);
        NodePin downTrigger = getBottomButton(wallSwitch, side);
        lst.addActionBinding(new ActionBinding(upTrigger, upButtonDownAction, upButtonUpAction));
        lst.addActionBinding(new ActionBinding(downTrigger, downButtonDownAction, downButtonUpAction));
    }

    protected static NodePin getBottomButton(WallSwitch wallSwitch, WallSwitch.Side side) {
        return (side == WallSwitch.Side.LEFT) ? wallSwitch.getLeftBottomButton() : wallSwitch.getRightBottomButton();
    }

    protected static NodePin getUpperButton(WallSwitch wallSwitch, WallSwitch.Side side) {
        return (side == WallSwitch.Side.LEFT) ? wallSwitch.getLeftUpperButton() : wallSwitch.getRightUpperButton();
    }

    public abstract void configure();

    public abstract String getConfigurationJs();

    protected void setupPir(SwitchListener lst, NodePin pirPin, String id, String name, Action activateAction, Action deactivateAction) {
        setupSensor(lst, pirPin, id, name, activateAction, deactivateAction, true);
    }

    protected void setupMagneticSensor(SwitchListener lst, NodePin pirPin, String id, String name, Action activateAction, Action deactivateAction) {
        setupSensor(lst, pirPin, id, name, activateAction, deactivateAction, false);
    }

    private void setupSensor(SwitchListener lst, NodePin pirPin, String id, String name, Action activateAction, Action deactivateAction, boolean logicalOneIsActivate) {
        PirStatus status = new PirStatus(id, name);
        Action[] activateActions = (activateAction != null) ? new Action[]{activateAction, status.getActivateAction()} : new Action[]{status.getActivateAction()};
        Action[] deactivateActions = (deactivateAction != null) ? new Action[]{deactivateAction, status.getDeactivateAction()} : new Action[]{status.getDeactivateAction()};
        if (logicalOneIsActivate) {
            lst.addActionBinding(new ActionBinding(pirPin, deactivateActions, activateActions));
        } else {
            lst.addActionBinding(new ActionBinding(pirPin, activateActions, deactivateActions));
        }
        pirStatusList.add(status);
    }
}