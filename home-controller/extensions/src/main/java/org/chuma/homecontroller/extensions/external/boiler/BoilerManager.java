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

    public BoilerManager(int refreshInternalMs, int maxUnusedRunTimeMs) {
        Options options = OptionsSingleton.getInstance();
        String ipAddress = options.get(CFG_BOILER_IP);
        boilerMonitor = new BoilerMonitor(options.get(CFG_BOILER_IP), refreshInternalMs, maxUnusedRunTimeMs);
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            log.info("BoilerMonitor disabled, no IP address configured");
            return;
        }
        boilerMonitor.start();

        setTargetTemp(options.getInt(CFG_BOILER_TARGET_TEMP));
        setOperatingTimes(options.get(CFG_BOILER_TIMES));
        setDisinfect(options.getBoolean(CFG_BOILER_DISINFECT));
        setEHeat(options.getBoolean(CFG_BOILER_EHEAT));
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

    public void turnOn() {
        try {
            BoilerController bc = boilerMonitor.getController();
            bc.refreshStatus();
            State state = bc.getState();
            int temp = (disinfect) ? 60 : targetTemp;
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

    public void turnOff() {
        try {
            BoilerController bc = boilerMonitor.getController();
            bc.refreshStatus();
            State state = bc.getState();

            if (state.isOn()) {
                log.debug("Turn on: ON -> OFF");
                bc.setPowerOn(false);
            } else {
                log.debug("Turn on: already OFF");
            }
            log.debug("Disabling boiler disinfect and e-heat");
            Options options = OptionsSingleton.getInstance();
            options.put(CFG_BOILER_DISINFECT, false);
            options.put(CFG_BOILER_EHEAT, false);
            optionsSaveInProgress = true;
            options.save();
        } catch (Exception e) {
            log.error("Turn off failed", e);
        } finally {
            optionsSaveInProgress = false;
        }
    }
}
