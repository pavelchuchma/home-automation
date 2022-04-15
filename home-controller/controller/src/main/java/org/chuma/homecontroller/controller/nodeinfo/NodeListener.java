package org.chuma.homecontroller.controller.nodeinfo;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.node.AbstractNodeListener;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.Pic;
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
    private final ConcurrentHashMap<String, ActionBinding> onInitializedMap = new ConcurrentHashMap<>();

    public static String createNodePinKey(int nodeId, Pin pin) {
        return String.format("%d:%s", nodeId, pin);
    }

    /**
     * Register action.
     */
    public void addActionBinding(ActionBinding binding) {
        addActionBinding(switchMap, binding, "");
    }

    /**
     * Register action called after node initialization. These actions are called each time the node
     * gets initialized. You can use the same binding as for {@link #addActionBinding(ActionBinding)}.
     * Note however, that this action does not indicate it happened now, just that pin is currently
     * in given state.
     */
    public void addOnInitializedActionBinding(ActionBinding binding) {
        addActionBinding(onInitializedMap, binding, "OnInit");
    }

    private static void addActionBinding(ConcurrentHashMap<String, ActionBinding> map, ActionBinding binding, String debugInfo) {
        String key = createNodePinKey(binding.getTrigger().getNode().getNodeId(), binding.getTrigger().getPin());
        ActionBinding existingMapping = map.put(key, binding);
        if (existingMapping != null) {
            throw new IllegalArgumentException("Node #" + binding.getTrigger().getNode().getNodeId() + ":" + binding.getTrigger().getPin().name() + " already bound");
        }
        log.info("{}ActionBinding '{}' added", debugInfo, binding);
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
        onInputChange(switchMap, node, pin, true, highDuration, "");
    }

    @Override
    public void onInputHigh(Node node, Pin pin, int lowDuration) {
        onInputChange(switchMap, node, pin, false, lowDuration, "");
    }

    private static void onInputChange(ConcurrentHashMap<String, ActionBinding> map, Node node, Pin pin, boolean lowState, final int timeSinceChange, String debugInfo) {
        String swKey = createNodePinKey(node.getNodeId(), pin);
        final ActionBinding sw = map.get(swKey);
        if (sw != null) {
            log.debug("Executing {}ActionBinding: {}", debugInfo, sw);
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
        String keyPrefix = node.getNodeId() + ":";
        // Masks to read ports A-E (although currently only A-D are supported by Pin)
        int[] toRead = new int[5];
        for (Entry<String, ActionBinding> e : onInitializedMap.entrySet()) {
            if (e.getKey().startsWith(keyPrefix)) {
                Pin pin = Pin.valueOf(e.getKey().substring(keyPrefix.length()));
                toRead[pin.getPortIndex()] |= pin.getBitMask();
            }
        }
        for (int port = 0; port < toRead.length; port++) {
            if (toRead[port] != 0) {
                try {
                    // Read port
                    int v = node.readMemory(Pic.PORTA + port);
                    // Generate events
                    for (int pin = 0, m = toRead[port]; m != 0; m >>= 1, v >>= 1, pin++) {
                        if ((m & 1) == 1) {
                            // Notify pin
                            onInputChange(onInitializedMap, node, Pin.get(port, pin), (v & 1) == 0, -1, "OnInit");
                        }
                    }
                } catch (IOException e) {
                    log.error("Error when reading PORT" + (char)('A' + port), e);
                }
            }
        }
    }
}