package org.chuma.homecontroller.controller.nodeinfo;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.node.AbstractNodeListener;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.Pin;
import org.chuma.homecontroller.controller.ActionBinding;
import org.chuma.homecontroller.controller.action.Action;

/**
 * Switch listener is registered by {@link NodeInfoRegistry} to all nodes to receive all notifications.
 * It dispatches notification to registered {@link Action} instances which are bound to single node pin
 * via {@link ActionBinding}.
 */
public class SwitchListener extends AbstractNodeListener {
    private static final Logger log = LoggerFactory.getLogger(SwitchListener.class.getName());
    private final ConcurrentHashMap<String, ActionBinding> switchMap = new ConcurrentHashMap<>();

    public static String createNodePinKey(int nodeId, Pin pin) {
        return String.format("%d:%s", nodeId, pin);
    }

    /**
     * Register action.
     */
    public void addActionBinding(ActionBinding binding) {
        String key = createNodePinKey(binding.getTrigger().getNode().getNodeId(), binding.getTrigger().getPin());
        ActionBinding existingMapping = switchMap.put(key, binding);
        if (existingMapping != null) {
            throw new IllegalArgumentException("Node #" + binding.getTrigger().getNode().getNodeId() + ":" + binding.getTrigger().getPin().name() + " already bound");
        }
        log.info("ActionBinding '{}' added", binding);
        if (binding.getButtonDownActions() != null) {
            log.info(" buttonDown");
            for (Action a : binding.getButtonDownActions()) {
                log.info("  - {}", a);
            }
        }
        if (binding.getButtonUpActions() != null) {
            log.info(" buttonUp");
            for (Action a : binding.getButtonUpActions()) {
                log.info("  - {}", a);
            }
        }
    }

    @Override
    public void onButtonDown(Node node, Pin pin, int upTime) {
        onButtonEvent(node, pin, true, upTime);
    }

    @Override
    public void onButtonUp(Node node, Pin pin, int downTime) {
        onButtonEvent(node, pin, false, downTime);
    }

    private void onButtonEvent(Node node, Pin pin, boolean buttonDown, final int previousDurationMs) {
        String swKey = createNodePinKey(node.getNodeId(), pin);
        final ActionBinding sw = switchMap.get(swKey);
        if (sw != null) {
            log.debug("Executing ActionBinding: {}", sw);
            Action[] actions = (buttonDown) ? sw.getButtonDownActions() : sw.getButtonUpActions();
            if (actions != null) {
                for (final Action a : actions) {
                    log.debug("-> action: {} of action type {}", (a.getActor() != null) ? a.getActor().getId() : "{null}", a.getClass().getSimpleName());
                    // TODO: Use executor?
                    new Thread(() -> {
                        try {
                            a.perform(previousDurationMs);
                        } catch (Exception e) {
                            log.error("Failed to perform actions of " + sw, e);
                        }
                    }).start();
                }
            }
        }
    }

    @Override
    public void onReboot(Node node, int pingCounter, int rconValue) {
        node.initialize();
    }
}