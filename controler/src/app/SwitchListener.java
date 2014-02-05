package app;

import controller.ActionBinding;
import controller.actor.Actor;
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
        switchMap.put(key, sw);
        log.info(String.format("ActionBinding '%s' added", sw));
        for (Actor a : sw.getButtonDownActors()) {
            log.info("  - " + a.toString());
        }
    }

    @Override
    public void onButtonDown(Node node, Pin pin) {
        String swKey = createNodePinKey(node.getNodeId(), pin);
        ActionBinding sw = switchMap.get(swKey);
        if (sw != null) {
            log.debug(String.format("Executing ActionBinding: %s", sw));
            for (Actor a : sw.getButtonDownActors()) {
                log.debug(String.format("-> action: %s", a.getId()));
                a.perform();
            }
        }
    }

    @Override
    public void onButtonUp(Node node, Pin pin, int downTime) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onReboot(Node node, int pingCounter, int rconValue) throws IOException, IllegalArgumentException {
        node.initialize();
    }
}