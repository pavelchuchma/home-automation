package controller.device;

import java.util.ArrayList;

import controller.actor.AbstractActor;
import controller.actor.ActorListener;
import controller.actor.IOnOffActor;
import node.NodePin;
import org.apache.log4j.Logger;

public class SwitchIndicator implements ActorListener {
    private static final int RETRY_COUNT = 2;
    static Logger log = Logger.getLogger(SwitchIndicator.class.getName());
    Mode mode;

    NodePin pin;
    //todo: Build on Actor after removal onValue from Actor (1 must stand for ON in all cases)
    ArrayList<IOnOffActor> actors = new ArrayList<>();
    private int lastSetValue = -1;

    public SwitchIndicator(NodePin pin, Mode mode) {
        this.pin = pin;
        this.mode = mode;
    }

    @Override
    public void onAction(IOnOffActor actor, boolean invert) {
//        log.debug(pin + ".onAction(" + actor + ", invert: " + invert + ", mode: " + mode + ")");
        if (!actors.contains(actor)) {
            throw new IllegalArgumentException("Cannot call onAction() with unregistered actor");
        }

        boolean val = false;
        switch (mode) {
            case SIGNAL_ALL_OFF:
                val = !isAnyOn();
                break;
            case SIGNAL_ANY_ON:
                val = isAnyOn();
                break;
        }

//        log.debug("  " + pin + " val: " + val + ", lastSetValue: " + lastSetValue);

        if (actors.size() == 1) {
            // blink only if bound to single actor
            val ^= invert;
        }
        int resultValue = (val) ? 0 : 1;
        if (resultValue != lastSetValue) {
//            log.debug("  setting " + pin + " to " + resultValue);
            if (AbstractActor.setPinValueImpl(pin, resultValue, RETRY_COUNT)) {
                lastSetValue = resultValue;
            }
        }
    }

    private boolean isAnyOn() {
        for (IOnOffActor a : actors) {
            if (a.isOn()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public NodePin getPin() {
        return pin;
    }

    @Override
    public void notifyRegistered(IOnOffActor actor) {
        if (actors.contains(actor)) {
            throw new IllegalArgumentException("Actor " + actor.toString() + " is already registered");
        }
        actors.add(actor);
    }

    public enum Mode {
        SIGNAL_ANY_ON, // false
        SIGNAL_ALL_OFF // true
    }
}