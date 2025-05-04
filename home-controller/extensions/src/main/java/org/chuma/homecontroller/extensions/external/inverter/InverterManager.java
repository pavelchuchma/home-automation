package org.chuma.homecontroller.extensions.external.inverter;

import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.utils.Options;
import org.chuma.homecontroller.extensions.external.inverter.impl.SolaxInverterModbusClient;
import org.chuma.homecontroller.extensions.external.utils.IntervalScheduler;

public class InverterManager {
    private static final String CFG_INVERTER_MANAGER_HIGH_TARIFF_BATTERY_RESERVE = "inverter.manager.high.tariff.battery.reserve";
    private static final String CFG_INVERTER_MANAGER_HIGH_TARIFF_TIMES = "inverter.manager.high.tariff.times";
    private static final String CFG_INVERTER_MANAGER_MINIMAL_SOC = "inverter.manager.minimal.soc";

    static Logger log = LoggerFactory.getLogger(InverterManager.class.getName());
    private final SolaxInverterModbusClient client;
    private final Options options;
    private int minimalSoc = -1;
    private int batteryReserve = -1;
    private final IntervalScheduler intervalScheduler = new IntervalScheduler(
            () -> applyMinBatterySoc(true),
            () -> applyMinBatterySoc(false)
    );

    public InverterManager(SolaxInverterModbusClient client, Options options) {
        this.client = client;
        this.options = options;

        setMinimalSoc(options.getInt(CFG_INVERTER_MANAGER_MINIMAL_SOC));
        setBatteryReserve(options.getInt(CFG_INVERTER_MANAGER_HIGH_TARIFF_BATTERY_RESERVE));
        setHighTariffRanges(options.get(CFG_INVERTER_MANAGER_HIGH_TARIFF_TIMES));
        applyConfiguration();

        options.addListener(new Options.OptionChangeListener() {
            @Override
            public void optionChanged(String key, String value) {
                if (CFG_INVERTER_MANAGER_MINIMAL_SOC.equals(key)) {
                    setMinimalSoc(Integer.parseInt(value));
                } else if (CFG_INVERTER_MANAGER_HIGH_TARIFF_BATTERY_RESERVE.equals(key)) {
                    setBatteryReserve(Integer.parseInt(value));
                } else if (CFG_INVERTER_MANAGER_HIGH_TARIFF_TIMES.equals(key)) {
                    setHighTariffRanges(value);
                }
            }

            @Override
            public void optionsSaved(Set<String> keys) {
                if (keys.contains(CFG_INVERTER_MANAGER_MINIMAL_SOC)
                        || keys.contains(CFG_INVERTER_MANAGER_HIGH_TARIFF_BATTERY_RESERVE)
                        || keys.contains(CFG_INVERTER_MANAGER_HIGH_TARIFF_TIMES)) {
                    applyConfiguration();
                }
            }
        });
    }

    public int getMinimalSoc() {
        return minimalSoc;
    }

    /**
     * Sets minimal battery charge level in percent.
     */
    public void setMinimalSoc(int minimalSoc) {
        Validate.inclusiveBetween(10, 100, minimalSoc);
        this.minimalSoc = minimalSoc;
    }

    public int getBatteryReserve() {
        return batteryReserve;
    }

    /**
     * Sets battery reserve for high tariff time. Battery SOC is set to setMinimalSoc+batteryReserve in low tariff
     * and to setMinimalSoc in high tariff.
     */
    public void setBatteryReserve(int batteryReserve) {
        log.debug("Setting battery reserve to {}", batteryReserve);
        Validate.inclusiveBetween(0, 90, batteryReserve);
        this.batteryReserve = batteryReserve;
    }

    public void setHighTariffRanges(String intervals) {
        log.debug("setHighTariffRanges({})", intervals);
        intervalScheduler.setIntervals(intervals);
    }

    public void applyConfiguration() {
        intervalScheduler.applyCallback();
    }

    void applyMinBatterySoc(boolean enteringHighTariff) {
        try {
            Validate.inclusiveBetween(10, 100, minimalSoc);
            Validate.inclusiveBetween(0, 90, batteryReserve);
            Validate.inclusiveBetween(10, 100, minimalSoc + batteryReserve);
            int minSoc = minimalSoc;
            if (enteringHighTariff) {
                log.debug("setMinBatterySoc: Entering high tariff, minSOC={}}", minSoc);
            } else {
                minSoc += batteryReserve;
                log.debug("setMinBatterySoc: Entering low tariff, minSOC={} ({}+{})}", minSoc, minimalSoc, batteryReserve);
            }

            int origValue = client.getState().getSelfUseMinimalSoc();
            if (minSoc != origValue) {
                client.setSelfUseMinimalSoc(minSoc);
                int storedValue = client.getState().getSelfUseMinimalSoc();
                if (storedValue != minSoc) {
                    log.error("Failed to set MinBatterySoc to {}, stored value is {}", minSoc, storedValue);
                }
            } else {
                log.debug("minBatterySoc already set to {}, no change needed", minSoc);
            }
        } catch (Exception e) {
            log.error("Failed to set MinBatterySoc", e);
        }
    }
}
