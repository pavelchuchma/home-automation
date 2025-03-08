package org.chuma.homecontroller.extensions.external.boiler;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.extensions.external.inverter.InverterManager;
import org.chuma.homecontroller.extensions.external.utils.IntervalScheduler;

public class BoilerManager {
    static Logger log = LoggerFactory.getLogger(InverterManager.class.getName());
    private final BoilerController bc;
    private int targetTemp = -1;
    private final IntervalScheduler intervalScheduler = new IntervalScheduler(
            this::turnOn,
            this::turnOff
    );

    public BoilerManager(BoilerController bc) {
        this.bc = bc;
    }

    public void setTargetTemp(int targetTemp) {
        Validate.inclusiveBetween(38, 60, targetTemp);
        this.targetTemp = targetTemp;
    }

    public void setOperatingTimes(String intervals) {
        log.debug("setOperatingTimes({})", intervals);
        intervalScheduler.setIntervals(intervals);
    }

    public void applyConfiguration() {
        intervalScheduler.applyCallback();
    }

    public void turnOn() {
        try {
            bc.refreshStatus();
            State state = bc.getState();
            if (state.getTargetTemp() != targetTemp) {
                log.debug("Turn on: targetTemp: {} -> {}", state.getTargetTemp(), targetTemp);
                bc.setTargetTemp(targetTemp);
            } else {
                log.debug("Turn on: targetTemp already set to {}", targetTemp);
            }

            if (!state.isOn()) {
                log.debug("Turn on: OFF -> ON");
                bc.setPowerOn(true);
            } else {
                log.debug("Turn on: already ON");
            }
        } catch (Exception e) {
            log.error("Turn on failed", e);
        }
    }

    public void turnOff() {
        try {
            bc.refreshStatus();
            State state = bc.getState();

            if (state.isOn()) {
                log.debug("Turn on: ON -> OFF");
                bc.setPowerOn(false);
            } else {
                log.debug("Turn on: already OFF");
            }
        } catch (Exception e) {
            log.error("Turn off failed", e);
        }
    }
}
