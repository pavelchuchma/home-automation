package app;

import controller.Switch;
import controller.actor.Actor;
import node.Node;
import node.Pin;
import nodeImpl.AbstractNodeListener;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class SwitchListener extends AbstractNodeListener {
    ConcurrentHashMap<String, Switch> switchMap = new ConcurrentHashMap<String, Switch>();
    static Logger log = Logger.getLogger(SwitchListener.class.getName());

    public static String createNodePinKey(int nodeId, Pin pin) {
        return String.format("%d:%s", nodeId, pin);
    }

    public void addSwitch(Switch sw) {
        String key = createNodePinKey(sw.getNodeId(), sw.getPin());
        switchMap.put(key, sw);
        log.info(String.format("Switch '%s' added", sw.getId()));
        for (Actor a : sw.getButtonDownActors()) {
            log.info("  - " + a.toString());
        }
    }

    @Override
    public void onButtonDown(Node node, Pin pin) {
        String swKey = createNodePinKey(node.getNodeId(), pin);
        Switch sw = switchMap.get(swKey);
        if (sw != null) {
            log.debug(String.format("Executing switch: %s", sw.getId()));
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
               /*
        int inputMasks = 0x00000000;
        int outputMasks = 0x00000000;
        // go through all switches to get initial settings
        for (Switch sw : switchMap.values()) {
            if (node.getNodeId() == sw.getNodeId()) {
                // todo: presunut rotaci jako actor
                inputMasks |= 2 << sw.getPin().ordinal();
            }
        }
        for (AbstractActor act : actorMap.values()) {
            if (node.getNodeId() == act.getNodeId()) {
                inputMasks |= act.getPinOutputMask();
            }
        }

        for (int i = 0; i < 4; i++) {
            int eventMask = (inputMasks >> i * 8) & 0xFF;
            if (eventMask != 0) {
                // todo: check TRIS to don't break CAN/UART settings
                node.setPortValue((char) ('A' + i), 0x00, 0x00, eventMask, 0xFF);
            }
        }
    */
    }
}