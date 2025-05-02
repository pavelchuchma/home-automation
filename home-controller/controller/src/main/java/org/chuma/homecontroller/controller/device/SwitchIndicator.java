package org.chuma.homecontroller.controller.device;

import java.util.ArrayList;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.node.PwmOutputNodePin;
import org.chuma.homecontroller.controller.action.condition.SensorDimCounter;
import org.chuma.homecontroller.controller.actor.ActorListener;
import org.chuma.homecontroller.controller.actor.IReadableOnOff;
import org.chuma.homecontroller.controller.actor.PwmActor;

public class SwitchIndicator implements ActorListener {
    private static final int RETRY_COUNT = 2;
    static Logger log = LoggerFactory.getLogger(SwitchIndicator.class.getName());
    final PwmOutputNodePin pin;
    Mode mode;
    private final double indicatorIntensity;
    ArrayList<IReadableOnOff> sources = new ArrayList<>();
    private boolean lastSetValue;

    SwitchIndicator(PwmOutputNodePin pin, Mode mode, double indicatorIntensity) {
        Validate.inclusiveBetween(0.01, 1, indicatorIntensity);
        this.pin = pin;
        this.mode = mode;
        this.indicatorIntensity = indicatorIntensity;
    }

    @Override
    public void onAction(IReadableOnOff source, Object actionData) {
//        log.debug(pin + ".onAction(" + actor + ", invert: " + invert + ", mode: " + mode + ")");
        if (!sources.contains(source)) {
            throw new IllegalArgumentException("Cannot call onAction() with unregistered source");
        }

        boolean value = false;
        if (mode == Mode.SIGNAL_ALL_OFF) {
            value = !isAnyOn();
        } else if (mode == Mode.SIGNAL_ANY_ON) {
            value = isAnyOn();
        }

//        log.debug("  " + pin + " value: " + value + ", lastSetValue: " + lastSetValue);
        if (actionData instanceof SensorDimCounter && sources.size() == 1) {
            value ^= (((SensorDimCounter)actionData).getCount() % 2 == 1);
        }
        if (value != lastSetValue) {
            if (PwmActor.setPinPwmValueImpl(pin, (value) ? 48 - Math.max(1, (int)(48 * indicatorIntensity)) : 48, RETRY_COUNT)) {
                lastSetValue = value;
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