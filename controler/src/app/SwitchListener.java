package app;

import controller.action.Action;
import controller.ActionBinding;
import node.Node;
import node.Pin;
import nodeImpl.AbstractNodeListener;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class SwitchListener extends AbstractNodeListener {
    ConcurrentHashMap<String, ActionBinding> switchMap = new ConcurrentHashMap<String, ActionBinding>();
    static Logger log = Logger.getLogger(SwitchListener.class.getName());

    public static String createNodePinKey(int nodeId, Pin pin) {
        return String.format("%d:%s", nodeId, pin);
    }

    public void addActionBinding(ActionBinding sw) {
        String key = createNodePinKey(sw.getTrigger().getNodeId(), sw.getTrigger().getPin());
        ActionBinding existingMapping = switchMap.put(key, sw);
        if (existingMapping != null) {
            throw new IllegalArgumentException("Node #" + sw.getTrigger().getNodeId() + ":" + sw.getTrigger().getPin().name() + " already bound");
        }
        log.info(String.format("ActionBinding '%s' added", sw.toString()));
        if (sw.getButtonDownActions() != null) {
            log.info(" buttonDown");
            for (Action a : sw.getButtonDownActions()) {
                log.info("  - " + a.toString());
            }
        }
        if (sw.getButtonUpActions() != null) {
            log.info(" buttonUp");
            for (Action a : sw.getButtonUpActions()) {
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
                    log.debug(String.format("-> action: %s of action type %s", a.getActor().getId(), a.getClass().getSimpleName()));
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                a.perform(previousDurationMs);
                            } catch (Exception e) {
                                log.error("Failed to perform actions of " + sw.toString(), e);
                            }
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