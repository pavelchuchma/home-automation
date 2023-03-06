package org.chuma.homecontroller.extensions.external.inverter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.extensions.external.inverter.impl.SolaxInverterRemoteClient;

public class InverterManager {
    static Logger log = LoggerFactory.getLogger(InverterManager.class.getName());
    private final SolaxInverterRemoteClient client;
    List<String> scheduledIds = new ArrayList<>();
    private int minimalSoc = -1;
    private int batteryReserve = -1;

    public InverterManager(SolaxInverterRemoteClient client) {
        this.client = client;
    }

    /**
     * Parse range string like "6:55-8:05;17:55-19:05" to pairs of times.
     */
    static List<LocalTime[]> parseTariffRanges(String str) {
        List<LocalTime[]> result = new ArrayList<>();
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm");
        for (String interval : str.split(";")) {
            String[] times = interval.split("-");
            Validate.isTrue(times.length == 2, "Invalid time interval '%s' of string %s", interval, str);
            result.add(new LocalTime[]{
                    LocalTime.parse(times[0], formatter),
                    LocalTime.parse(times[1], formatter)
            });
        }
        return result;
    }

    /**
     * Sets minimal battery charge level in percent.
     */
    public void setMinimalSoc(int minimalSoc) {
        Validate.inclusiveBetween(10, 100, minimalSoc);
        this.minimalSoc = minimalSoc;
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
        final List<LocalTime[]> tariffRanges = parseTariffRanges(intervals);
        Scheduler scheduler = Scheduler.getInstance();
        scheduler.removeScheduledTasks(scheduledIds);
        scheduledIds.clear();
        for (LocalTime[] interval : tariffRanges) {
            scheduledIds.add(scheduler.scheduleTask(interval[0], () -> applyMinBatterySoc(true)));
            scheduledIds.add(scheduler.scheduleTask(interval[1], () -> applyMinBatterySoc(false)));
        }
    }

    void applyMinBatterySoc(boolean enteringHighTariff) {
        try {
            Validate.inclusiveBetween(10, 100, minimalSoc);
            Validate.inclusiveBetween(0, 90, batteryReserve);
            int minSoc = minimalSoc;
            if (enteringHighTariff) {
                log.debug("setMinBatterySoc: Entering high tariff, minSOC={}}", minSoc);
            } else {
                minSoc += batteryReserve;
                log.debug("setMinBatterySoc: Entering low tariff, minSOC={} ({}+{})}", minSoc, minimalSoc, batteryReserve);
            }

            client.setSelfUseMinimalSoc(minSoc);

            int storedValue = client.getConfiguration().getSelfUseMinimalSoc();
            if (storedValue != minSoc) {
                log.error("Failed to set MinBatterySoc to {}, stored value is {}", minSoc, storedValue);
            }
            log.debug("setMinBatterySoc: done");
        } catch (Exception e) {
            log.error("Failed to set MinBatterySoc", e);
        }
    }
}
