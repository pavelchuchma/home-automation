package org.chuma.homecontroller.app.configurator;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.app.servlet.Servlet;
import org.chuma.homecontroller.app.servlet.pages.Page;
import org.chuma.homecontroller.app.servlet.simulation.SimulationPage;
import org.chuma.homecontroller.app.servlet.simulation.SimulationWebSocketHandler;
import org.chuma.homecontroller.app.servlet.ws.WebSocketHandler;
import org.chuma.homecontroller.base.node.NodePin;
import org.chuma.homecontroller.base.node.OutputNodePin;
import org.chuma.homecontroller.base.packet.IPacketUartIO;
import org.chuma.homecontroller.base.packet.simulation.SimulatedPacketUartIO;
import org.chuma.homecontroller.controller.ActionBinding;
import org.chuma.homecontroller.controller.PirStatus;
import org.chuma.homecontroller.controller.action.Action;
import org.chuma.homecontroller.controller.action.LouversActionGroup;
import org.chuma.homecontroller.controller.action.PwmActionGroup;
import org.chuma.homecontroller.controller.action.SwitchOffAction;
import org.chuma.homecontroller.controller.actor.ActorListener;
import org.chuma.homecontroller.controller.actor.IOnOffActor;
import org.chuma.homecontroller.controller.actor.LddActor;
import org.chuma.homecontroller.controller.actor.OnOffActor;
import org.chuma.homecontroller.controller.actor.PwmActor;
import org.chuma.homecontroller.controller.controller.LouversController;
import org.chuma.homecontroller.controller.controller.LouversControllerImpl;
import org.chuma.homecontroller.controller.device.LddBoardDevice;
import org.chuma.homecontroller.controller.device.SwitchIndicator;
import org.chuma.homecontroller.controller.device.WallSwitch;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfo;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoRegistry;
import org.chuma.homecontroller.controller.nodeinfo.NodeListener;

public abstract class AbstractConfigurator {
    static Logger log = LoggerFactory.getLogger(AbstractConfigurator.class.getName());
    final protected NodeInfoRegistry nodeInfoRegistry;
    final protected List<PirStatus> pirStatusList = new ArrayList<>();
    final protected List<IOnOffActor> onOffActors = new ArrayList<>();
    final protected List<LouversController> louversControllers = new ArrayList<>();
    final protected List<LddActor> lddActors = new ArrayList<>();
    Servlet servlet;

    public AbstractConfigurator(NodeInfoRegistry nodeInfoRegistry) {
        this.nodeInfoRegistry = nodeInfoRegistry;
    }

    protected static void configurePwmLights(NodeListener lst, WallSwitch wallSwitch, WallSwitch.Side side, double switchOnValue, PwmActor... pwmActors) {
        configurePwmLightsImpl(lst, wallSwitch, side, switchOnValue, pwmActors, null);
    }

    protected static void configurePwmLightsImpl(NodeListener lst, WallSwitch wallSwitch, WallSwitch.Side side, double switchOnValue, PwmActor[] pwmActors, IOnOffActor[] switchOffOnlyActors) {
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
     * @param maxLightCurrent Maximal current in Amperes for light
     */
    protected LddActor addLddLight(String id, String label, LddBoardDevice.LddNodePin pin, double maxLightCurrent, ActorListener... actorListeners) {
        log.debug("Adding LDD Light: {}, {}, {}, {}, {}, {}", pin.getDeviceName(), pin.getPin().getPinIndex(), id, label, pin.getMaxLddCurrent(), maxLightCurrent);
        LddActor lddActor = new LddActor(id, label, pin, maxLightCurrent, actorListeners);
        lddActors.add(lddActor);
        return lddActor;
    }

    static void configureLouvers(NodeListener lst, WallSwitch wallSwitch, WallSwitch.Side side, LouversController... louversControllers) {
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

    public Servlet getServlet() {
        return servlet;
    }

    protected void setupPir(NodeListener lst, NodePin pirPin, String id, String name, Action[] activateActions, Action[] deactivateActions) {
        setupSensor(lst, pirPin, id, name, activateActions, deactivateActions, true);
    }

    protected void setupPir(NodeListener lst, NodePin pirPin, String id, String name, Action activateAction, Action deactivateAction) {
        setupPir(lst, pirPin, id, name, toArray(activateAction), toArray(deactivateAction));
    }

    protected void setupMagneticSensor(NodeListener lst, NodePin pirPin, String id, String name, Action[] activateActions, Action[] deactivateActions) {
        setupSensor(lst, pirPin, id, name, activateActions, deactivateActions, false);
    }

    protected void setupMagneticSensor(NodeListener lst, NodePin pirPin, String id, String name, Action activateAction, Action deactivateAction) {
        setupMagneticSensor(lst, pirPin, id, name, toArray(activateAction), toArray(deactivateAction));
    }

    protected IOnOffActor addOnOffActor(String id, String label, OutputNodePin output, ActorListener... actorListeners) {
        IOnOffActor actor = new OnOffActor(id, label, output, actorListeners);
        onOffActors.add(actor);
        return actor;
    }

    abstract int getLouversMaxOffsetMs();
    protected LouversController addLouversController(String id, String name, OutputNodePin relayUp, OutputNodePin relayDown, int downPositionMs) {
        LouversController controller = new LouversControllerImpl(id, name, relayUp, relayDown, downPositionMs, getLouversMaxOffsetMs());
        louversControllers.add(controller);
        return controller;
    }

    private void setupSensor(NodeListener lst, NodePin pirPin, String id, String name, Action[] activateActions, Action[] deactivateActions, boolean logicalOneIsActivate) {
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

    /**
     * Configure simulator into list of pages and web socket handlers if using {@link SimulatedPacketUartIO}.
     *
     * @param initFromRegistry initialize simulated nodes from {@link NodeInfoRegistry}
     */
    @SuppressWarnings("unused")
    protected void configureSimulator(List<Page> pages, List<WebSocketHandler> wsHandlers, boolean initFromRegistry) {
        IPacketUartIO packetUartIO = nodeInfoRegistry.getPacketUartIO();
        if (packetUartIO instanceof SimulatedPacketUartIO) {
            SimulatedPacketUartIO sim = (SimulatedPacketUartIO)packetUartIO;
            if (initFromRegistry) {
                for (NodeInfo node : nodeInfoRegistry.getNodeInfos()) {
                    if (sim.getSimulatedNode(node.getNode().getNodeId()) == null) {
                        // Do not register already registered nodes - user might have already registered and initialized some simulated nodes
                        sim.registerNode(node.getNode());
                    }
                }
            }
            pages.add(new SimulationPage(pages, sim));
            wsHandlers.add(new SimulationWebSocketHandler(sim));
        }
    }
}
