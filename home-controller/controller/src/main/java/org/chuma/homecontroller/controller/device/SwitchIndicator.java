package org.chuma.homecontroller.controller.device;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.node.OutputNodePin;
import org.chuma.homecontroller.controller.action.condition.SensorDimCounter;
import org.chuma.homecontroller.controller.actor.AbstractPinActor;
import org.chuma.homecontroller.controller.actor.ActorListener;
import org.chuma.homecontroller.controller.actor.IReadableOnOff;

public class SwitchIndicator implements ActorListener {
    private static final int RETRY_COUNT = 2;
    static Logger log = LoggerFactory.getLogger(SwitchIndicator.class.getName());
    final OutputNodePin pin;
    Mode mode;
    ArrayList<IReadableOnOff> sources = new ArrayList<>();
    private int lastSetValue = -1;

    public SwitchIndicator(OutputNodePin pin, Mode mode) {
        this.pin = pin;
        this.mode = mode;
    }


    @Override
    public void onAction(IReadableOnOff source, Object actionData) {
//        log.debug(pin + ".onAction(" + actor + ", invert: " + invert + ", mode: " + mode + ")");
        if (!sources.contains(source)) {
            throw new IllegalArgumentException("Cannot call onAction() with unregistered source");
        }

        boolean val = false;
        if (mode == Mode.SIGNAL_ALL_OFF) {
            val = !isAnyOn();
        } else if (mode == Mode.SIGNAL_ANY_ON) {
            val = isAnyOn();
        }

//        log.debug("  " + pin + " val: " + val + ", lastSetValue: " + lastSetValue);
        if (actionData instanceof SensorDimCounter && sources.size() == 1) {
            val ^= (((SensorDimCounter)actionData).getCount() % 2 == 1);
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
        SIGNAL_ANY_ON,
        SIGNAL_ALL_OFF
    }
}