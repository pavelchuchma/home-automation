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
 * Node listener is registered by {@link NodeInfoRegistry} to all nodes to receive all notifications.
 * It dispatches notification to registered {@link Action} instances which are bound to single node pin
 * via {@link ActionBinding}.
 */
public class NodeListener extends AbstractNodeListener {
    private static final Logger log = LoggerFactory.getLogger(NodeListener.class.getName());
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
        if (binding.getOnInputLowActions() != null) {
            log.info(" inputLow");
            for (Action a : binding.getOnInputLowActions()) {
                log.info("  - {}", a);
            }
        }
        if (binding.getOnInputHighActions() != null) {
            log.info(" InputHigh");
            for (Action a : binding.getOnInputHighActions()) {
                log.info("  - {}", a);
            }
        }
    }

    @Override
    public void onInputLow(Node node, Pin pin, int highDuration) {
        onInputChange(node, pin, true, highDuration);
    }

    @Override
    public void onInputHigh(Node node, Pin pin, int lowDuration) {
        onInputChange(node, pin, false, lowDuration);
    }

    private void onInputChange(Node node, Pin pin, boolean lowState, final int timeSinceChange) {
        String swKey = createNodePinKey(node.getNodeId(), pin);
        final ActionBinding sw = switchMap.get(swKey);
        if (sw != null) {
            log.debug("Executing ActionBinding: {}", sw);
            Action[] actions = (lowState) ? sw.getOnInputLowActions() : sw.getOnInputHighActions();
            if (actions != null) {
                for (final Action a : actions) {
                    log.debug("-> action: {} of action type {}", (a.getActor() != null) ? a.getActor().getId() : "{null}", a.getClass().getSimpleName());
                    // TODO: Use executor?
                    new Thread(() -> {
                        try {
                            a.perform(timeSinceChange);
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

    @Override
    public void onInitialized(Node node) {
        // TODO: Here we should call some listeners but we don't have any interface for them yet. Or, we can read registered input pins and call registered listeners.
    }
}