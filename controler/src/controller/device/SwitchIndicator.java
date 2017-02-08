package controller.device;

import java.util.ArrayList;

import controller.actor.AbstractPinActor;
import controller.actor.ActorListener;
import controller.actor.IReadableOnOff;
import node.NodePin;
import org.apache.log4j.Logger;

public class SwitchIndicator implements ActorListener {
    private static final int RETRY_COUNT = 2;
    static Logger log = Logger.getLogger(SwitchIndicator.class.getName());
    Mode mode;

    NodePin pin;
    //todo: Build on Actor after removal onValue from Actor (1 must stand for ON in all cases)
    ArrayList<IReadableOnOff> sources = new ArrayList<>();
    private int lastSetValue = -1;

    public SwitchIndicator(NodePin pin, Mode mode) {
        this.pin = pin;
        this.mode = mode;
    }

    @Override
    public void onAction(IReadableOnOff source, boolean invert) {
//        log.debug(pin + ".onAction(" + actor + ", invert: " + invert + ", mode: " + mode + ")");
        if (!sources.contains(source)) {
            throw new IllegalArgumentException("Cannot call onAction() with unregistered source");
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

        if (sources.size() == 1) {
            // blink only if bound to single source
            val ^= invert;
        }
        int resultValue = (val) ? 0 : 1;
        if (resultValue != lastSetValue) {
//            log.debug("  setting " + pin + " to " + resultValue);
            if (AbstractPinActor.setPinValueImpl(pin, resultValue, RETRY_COUNT)) {
                lastSetValue = resultValue;
            }
        }
    }

    private boolean isAnyOn() {
        for (IReadableOnOff a : sources) {
            if (a.isOn()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addSource(IReadableOnOff source) {
        if (sources.contains(source)) {
            throw new IllegalArgumentException("Source " + source.toString() + " is already registered");
        }
        sources.add(source);
    }

    public enum Mode {
        SIGNAL_ANY_ON, // false
        SIGNAL_ALL_OFF // true
    }
}