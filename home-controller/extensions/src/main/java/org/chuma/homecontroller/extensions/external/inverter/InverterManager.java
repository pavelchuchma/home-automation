package org.chuma.homecontroller.extensions.external.inverter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.extensions.external.inverter.impl.SolaxInverterModbusClient;
import org.chuma.homecontroller.extensions.external.utils.TimeRange;

public class InverterManager {
    static Logger log = LoggerFactory.getLogger(InverterManager.class.getName());
    private final SolaxInverterModbusClient client;
    List<String> scheduledIds = new ArrayList<>();
    private List<TimeRange> tariffRanges;
    private int minimalSoc = -1;
    private int batteryReserve = -1;

    public InverterManager(SolaxInverterModbusClient client) {
        this.client = client;
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
        Validate.inclusiveBetween(0, 90, batteryReserve);
        this.batteryReserve = batteryReserve;
    }

    public void setHighTariffRanges(String intervals) {
        log.debug("setHighTariffRanges([{}])", intervals);
        tariffRanges = TimeRange.parseTariffRanges(intervals);
        Scheduler scheduler = Scheduler.getInstance();
        scheduler.removeScheduledTasks(scheduledIds);
        scheduledIds.clear();
        for (TimeRange range : tariffRanges) {
            scheduledIds.add(scheduler.scheduleTask(range.from, () -> applyMinBatterySoc(true)));
            scheduledIds.add(scheduler.scheduleTask(range.to, () -> applyMinBatterySoc(false)));
        }
    }

    private boolean isInHighTariff() {
        LocalTime now = LocalTime.from(LocalDateTime.now());
        for (TimeRange range : tariffRanges) {
            if (range.from.isBefore(range.to)) {
                if (range.from.isBefore(now) && range.to.isAfter(now)) {
                    return true;
                }
            } else {
                // during midnight
                if (range.from.isBefore(now) || range.to.isAfter(now)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void applyConfiguration() {
        boolean highTariff = isInHighTariff();
        try {
            applyMinBatterySoc(highTariff);
        } catch (Exception e) {
            log.error("Failed to applyConfiguration", e);
        }
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

            client.setSelfUseMinimalSoc(minSoc);

            int storedValue = client.getState().getSelfUseMinimalSoc();
            if (storedValue != minSoc) {
                log.error("Failed to set MinBatterySoc to {}, stored value is {}", minSoc, storedValue);
            }
            log.debug("setMinBatterySoc: done");
        } catch (Exception e) {
            log.error("Failed to set MinBatterySoc", e);
        }
    }
}
