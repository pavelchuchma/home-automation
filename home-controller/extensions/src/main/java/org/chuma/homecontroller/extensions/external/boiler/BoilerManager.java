package org.chuma.homecontroller.extensions.external.boiler;

import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.utils.Options;
import org.chuma.homecontroller.base.utils.OptionsSingleton;
import org.chuma.homecontroller.extensions.external.inverter.InverterManager;
import org.chuma.homecontroller.extensions.external.utils.IntervalScheduler;

public class BoilerManager {
    private static final String CFG_BOILER_IP = "boiler.ip";
    private static final String CFG_BOILER_TIMES = "boiler.times";
    private static final String CFG_BOILER_TARGET_TEMP = "boiler.target.temp";
    private static final String CFG_BOILER_DISINFECT = "boiler.disinfect";
    private static final String CFG_BOILER_EHEAT = "boiler.eheat";
    private static final int DISINFECT_TEMP = 60;

    enum Correction {
        NONE, TURN_ON, TURN_OFF
    }

    static Logger log = LoggerFactory.getLogger(InverterManager.class.getName());
    private final BoilerMonitor boilerMonitor;
    private int targetTemp = -1;
    private boolean disinfect = false;
    private boolean eHeat = false;
    private boolean optionsSaveInProgress = false;
    private final IntervalScheduler intervalScheduler = new IntervalScheduler(
            this::turnOn,
            this::turnOff
    );

    public BoilerManager(int refreshIntervalMs, int fastRateDurationMs, int idleRefreshIntervalMs) {
        Options options = OptionsSingleton.getInstance();
        String ipAddress = options.get(CFG_BOILER_IP);
        boilerMonitor = new BoilerMonitor(ipAddress, refreshIntervalMs, fastRateDurationMs, idleRefreshIntervalMs,
                this::checkState);
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            log.info("BoilerMonitor disabled, no IP address configured");
            return;
        }

        setTargetTemp(options.getInt(CFG_BOILER_TARGET_TEMP));
        setOperatingTimes(options.get(CFG_BOILER_TIMES));
        setDisinfect(options.getBoolean(CFG_BOILER_DISINFECT));
        setEHeat(options.getBoolean(CFG_BOILER_EHEAT));
        // started after the configuration is loaded, checkState() needs it
        boilerMonitor.start();
        applyConfiguration();

        options.addListener(new Options.OptionChangeListener() {
            @Override
            public void optionChanged(String key, String value) {
                if (CFG_BOILER_TARGET_TEMP.equals(key)) {
                    setTargetTemp(Integer.parseInt(value));
                } else if (CFG_BOILER_TIMES.equals(key)) {
                    setOperatingTimes(value);
                } else if (CFG_BOILER_DISINFECT.equals(key)) {
                    setDisinfect(Boolean.parseBoolean(value));
                } else if (CFG_BOILER_EHEAT.equals(key)) {
                    setEHeat(Boolean.parseBoolean(value));
                }
            }

            @Override
            public void optionsSaved(Set<String> keys) {
                if (!optionsSaveInProgress && (keys.contains(CFG_BOILER_TARGET_TEMP) || keys.contains(CFG_BOILER_TIMES)
                        || keys.contains(CFG_BOILER_DISINFECT) || keys.contains(CFG_BOILER_EHEAT))) {
                    applyConfiguration();
                }
            }
        });
    }

    public BoilerMonitor getBoilerMonitor() {
        return boilerMonitor;
    }

    public int getTargetTemp() {
        return targetTemp;
    }

    public boolean isDisinfect() {
        return disinfect;
    }

    public boolean isEHeat() {
        return eHeat;
    }

    /**
     * @return true when the current time is inside one of the configured operating intervals
     */
    public boolean isScheduledOn() {
        return intervalScheduler.isInInterval();
    }

    public void setTargetTemp(int targetTemp) {
        Validate.inclusiveBetween(38, 60, targetTemp);
        this.targetTemp = targetTemp;
    }

    public void setOperatingTimes(String intervals) {
        log.debug("setOperatingTimes({})", intervals);
        intervalScheduler.setIntervals(intervals);
    }

    public void setDisinfect(boolean disinfect) {
        log.debug("setDisinfect({})", disinfect);
        this.disinfect = disinfect;
    }

    public void setEHeat(boolean eHeat) {
        log.debug("setEHeat({})", eHeat);
        this.eHeat = eHeat;
    }

    public void applyConfiguration() {
        intervalScheduler.applyCallback();
    }

    private int getDesiredTemp() {
        return (disinfect) ? DISINFECT_TEMP : targetTemp;
    }

    /**
     * Compares the boiler state with the schedule: inside an interval the boiler must be on with the desired
     * target temperature, outside it must be off. eHeat is not checked.
     */
    static Correction getCorrection(boolean scheduledOn, int desiredTemp, State state) {
        if (scheduledOn) {
            return (!state.isOn() || state.getTargetTemp() != desiredTemp) ? Correction.TURN_ON : Correction.NONE;
        }
        return (state.isOn()) ? Correction.TURN_OFF : Correction.NONE;
    }

    /**
     * Called after each periodic state refresh. Repeats turnOn() / turnOff() when the boiler differs from the
     * schedule, e.g. after a failed write at the interval edge or a manual change on the panel. Both re-read
     * the state before writing, so a stale state or a concurrent interval edge ends as a no-op.
     */
    void checkState(State state) {
        boolean scheduledOn = isScheduledOn();
        int desiredTemp = getDesiredTemp();
        switch (getCorrection(scheduledOn, desiredTemp, state)) {
            case TURN_ON -> {
                log.warn("Boiler differs from the schedule (on: {}, targetTemp: {}, desired: {}), turning on",
                        state.isOn(), state.getTargetTemp(), desiredTemp);
                turnOn();
            }
            case TURN_OFF -> {
                log.warn("Boiler is on outside the scheduled intervals, turning off");
                turnOff();
            }
            case NONE -> {
            }
        }
    }

    public synchronized void turnOn() {
        try {
            BoilerController bc = boilerMonitor.getController();
            bc.refreshStatus();
            State state = bc.getState();
            int temp = getDesiredTemp();
            if (state.getTargetTemp() != temp) {
                log.debug("Turn on: targetTemp: {} -> {}", state.getTargetTemp(), temp);
                bc.setTargetTemp(temp);
            } else {
                log.debug("Turn on: targetTemp already set to {}", temp);
            }

            if (!state.isOn()) {
                log.debug("Turn on: OFF -> ON");
                bc.setPowerOn(true);
            } else {
                log.debug("Turn on: already ON");
            }

            if (eHeat && !state.isEHeat()) {
                bc.turnEHeatOn();
            }
        } catch (Exception e) {
            log.error("Turn on failed", e);
        }
    }

    public synchronized void turnOff() {
        try {
            BoilerController bc = boilerMonitor.getController();
            bc.refreshStatus();
            State state = bc.getState();

            if (state.isOn()) {
                log.debug("Turn off: ON -> OFF");
                bc.setPowerOn(false);

                // One-shot flags are cleared only when we actually power the boiler down,
                // so that enabling disinfect/eheat outside an interval survives until the
                // next scheduled turnOn() instead of being wiped by applyCallback().
                log.debug("Disabling boiler disinfect and e-heat");
                Options options = OptionsSingleton.getInstance();
                options.put(CFG_BOILER_DISINFECT, false);
                options.put(CFG_BOILER_EHEAT, false);
                optionsSaveInProgress = true;
                options.save();
            } else {
                log.debug("Turn off: already OFF");
            }
        } catch (Exception e) {
            log.error("Turn off failed", e);
        } finally {
            optionsSaveInProgress = false;
        }
    }
}
