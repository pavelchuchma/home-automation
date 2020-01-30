package app;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import controller.ActionBinding;
import controller.action.Action;
import node.Node;
import node.Pin;
import nodeImpl.AbstractNodeListener;
import org.apache.log4j.Logger;

public class SwitchListener extends AbstractNodeListener {
    static Logger log = Logger.getLogger(SwitchListener.class.getName());
    ConcurrentHashMap<String, ActionBinding> switchMap = new ConcurrentHashMap<String, ActionBinding>();

    public static String createNodePinKey(int nodeId, Pin pin) {
        return String.format("%d:%s", nodeId, pin);
    }

    public void addActionBinding(ActionBinding binding) {
        String key = createNodePinKey(binding.getTrigger().getNodeId(), binding.getTrigger().getPin());
        ActionBinding existingMapping = switchMap.put(key, binding);
        if (existingMapping != null) {
            throw new IllegalArgumentException("Node #" + binding.getTrigger().getNodeId() + ":" + binding.getTrigger().getPin().name() + " already bound");
        }
        log.info(String.format("ActionBinding '%s' added", binding.toString()));
        if (binding.getButtonDownActions() != null) {
            log.info(" buttonDown");
            for (Action a : binding.getButtonDownActions()) {
                log.info("  - " + a.toString());
            }
        }
        if (binding.getButtonUpActions() != null) {
            log.info(" buttonUp");
            for (Action a : binding.getButtonUpActions()) {
                log.info("  - " + a.toString());
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
            log.debug(String.format("Executing ActionBinding: %s", sw));
            Action[] actions = (buttonDown) ? sw.getButtonDownActions() : sw.getButtonUpActions();
            if (actions != null) {
                for (final Action a : actions) {
                    log.debug(String.format("-> action: %s of action type %s", (a.getActor() != null) ? a.getActor().getId() : "{null}", a.getClass().getSimpleName()));
                    new Thread(() -> {
                        try {
                            a.perform(previousDurationMs);
                        } catch (Exception e) {
                            log.error("Failed to perform actions of " + sw.toString(), e);
                        }
                    }).start();
                }
            }
        }
    }

    @Override
    public void onReboot(Node node, int pingCounter, int rconValue) throws IOException, IllegalArgumentException {
        node.initialize();
    }
}