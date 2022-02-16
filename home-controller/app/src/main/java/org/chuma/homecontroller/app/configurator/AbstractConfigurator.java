package org.chuma.homecontroller.app.configurator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.app.servlet.Servlet;
import org.chuma.homecontroller.base.node.NodePin;
import org.chuma.homecontroller.controller.ActionBinding;
import org.chuma.homecontroller.controller.PirStatus;
import org.chuma.homecontroller.controller.action.Action;
import org.chuma.homecontroller.controller.action.LouversActionGroup;
import org.chuma.homecontroller.controller.action.PwmActionGroup;
import org.chuma.homecontroller.controller.action.SwitchOffAction;
import org.chuma.homecontroller.controller.actor.ActorListener;
import org.chuma.homecontroller.controller.actor.IOnOffActor;
import org.chuma.homecontroller.controller.actor.PwmActor;
import org.chuma.homecontroller.controller.controller.LouversController;
import org.chuma.homecontroller.controller.device.LddBoardDevice;
import org.chuma.homecontroller.controller.device.SwitchIndicator;
import org.chuma.homecontroller.controller.device.WallSwitch;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoCollector;
import org.chuma.homecontroller.controller.nodeinfo.SwitchListener;

public abstract class AbstractConfigurator {
    static Logger log = LoggerFactory.getLogger(AbstractConfigurator.class.getName());
    protected NodeInfoCollector nodeInfoCollector;
    protected List<PirStatus> pirStatusList = new ArrayList<>();
    Servlet servlet;

    public AbstractConfigurator(NodeInfoCollector nodeInfoCollector) {
        this.nodeInfoCollector = nodeInfoCollector;
    }

    protected static void configurePwmLights(SwitchListener lst, WallSwitch wallSwitch, WallSwitch.Side side, double switchOnValue, PwmActor... pwmActors) {
        configurePwmLightsImpl(lst, wallSwitch, side, switchOnValue, pwmActors, null);
    }

    protected static void configurePwmLightsImpl(SwitchListener lst, WallSwitch wallSwitch, WallSwitch.Side side, double switchOnValue, PwmActor[] pwmActors, IOnOffActor[] switchOffOnlyActors) {
        List<Action> upperButtonUpActions = new ArrayList<>();
        List<Action> upperButtonDownActions = new ArrayList<>();
        List<Action> downButtonUpActions = new ArrayList<>();
        List<Action> downButtonDownActions = new ArrayList<>();

        for (PwmActor pwmActor : pwmActors) {
            PwmActionGroup actionGroup = new PwmActionGroup(pwmActor, switchOnValue);
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

    /**
     * @param pwmActors
     * @param id
     * @param label
     * @param pin
     * @param maxCurrentAmp  Maximal current in Amperes for light
     * @param actorListeners
     * @return
     */
    static PwmActor addLddLight(List<PwmActor> pwmActors, String id, String label, LddBoardDevice.LddNodePin pin, double maxCurrentAmp, ActorListener... actorListeners) {
        log.debug("Adding LDD Light: {}, {}, {}, {}, {}, {}", pin.getDeviceName(), pin.getPin().getPinIndex(), id, label, pin.getMaxLddCurrent(), maxCurrentAmp);
        PwmActor pwmActor = new PwmActor(id, label, pin, maxCurrentAmp, actorListeners);
        pwmActors.add(pwmActor);
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

    private static Action[] toArray(Action a) {
        return (a != null) ? new Action[]{a} : null;
    }

    private static Action[] toArray(List<Action> list) {
        return list.toArray(new Action[0]);
    }

    public abstract void configure();

    Iterable<PwmActor> getPwmActors(Iterable<Action> lightActions) {
        Map<String, PwmActor> pwmActorMap = new TreeMap<>();
        for (Action lightAction : lightActions) {
            PwmActor actor = (PwmActor) lightAction.getActor();
            PwmActor old = pwmActorMap.put(actor.getId(), actor);
            if (old != null && old != actor) {
                throw new RuntimeException("Id of actor '" + actor.getId() + "' is not unique");
            }
        }
        return pwmActorMap.values();
    }

    public abstract String getConfigurationJs();

    public Servlet getServlet() {
        return servlet;
    }

    protected void setupPir(SwitchListener lst, NodePin pirPin, String id, String name, Action[] activateActions, Action[] deactivateActions) {
        setupSensor(lst, pirPin, id, name, activateActions, deactivateActions, true);
    }

    protected void setupPir(SwitchListener lst, NodePin pirPin, String id, String name, Action activateAction, Action deactivateAction) {
        setupPir(lst, pirPin, id, name, toArray(activateAction), toArray(deactivateAction));
    }

    protected void setupMagneticSensor(SwitchListener lst, NodePin pirPin, String id, String name, Action[] activateActions, Action[] deactivateActions) {
        setupSensor(lst, pirPin, id, name, activateActions, deactivateActions, false);
    }

    protected void setupMagneticSensor(SwitchListener lst, NodePin pirPin, String id, String name, Action activateAction, Action deactivateAction) {
        setupMagneticSensor(lst, pirPin, id, name, toArray(activateAction), toArray(deactivateAction));
    }

    private void setupSensor(SwitchListener lst, NodePin pirPin, String id, String name, Action[] activateActions, Action[] deactivateActions, boolean logicalOneIsActivate) {
        PirStatus status = new PirStatus(id, name);
        Action[] activateActionArray = ArrayUtils.addAll(activateActions, status.getActivateAction());
        Action[] deactivateActionArray = ArrayUtils.addAll(deactivateActions, status.getDeactivateAction());
        if (logicalOneIsActivate) {
            lst.addActionBinding(new ActionBinding(pirPin, deactivateActionArray, activateActionArray));
        } else {
            lst.addActionBinding(new ActionBinding(pirPin, activateActionArray, deactivateActionArray));
        }
        pirStatusList.add(status);
    }
}
