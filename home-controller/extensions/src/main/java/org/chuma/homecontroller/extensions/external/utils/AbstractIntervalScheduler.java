package org.chuma.homecontroller.extensions.external.utils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import org.chuma.homecontroller.extensions.external.inverter.Scheduler;

public abstract class AbstractIntervalScheduler {
    List<String> scheduledIds = new ArrayList<>();
    private List<TimeRange> tariffRanges;

    private static class TimeRange {
        public final LocalTime from;
        public final LocalTime to;

        TimeRange(LocalTime from, LocalTime to) {
            this.from = from;
            this.to = to;
        }
    }

    public void setIntervals(String intervals) {
        tariffRanges = parseTariffRanges(intervals);
        Scheduler scheduler = Scheduler.getInstance();
        scheduler.removeScheduledTasks(scheduledIds);
        scheduledIds.clear();
        for (TimeRange range : tariffRanges) {
            scheduledIds.add(scheduler.scheduleTask(range.from, this::onIntervalStart));
            scheduledIds.add(scheduler.scheduleTask(range.to, this::onIntervalEnd));
        }
    }

    public boolean isInInterval() {
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

    /**
     * Parses range string like "6:55-8:05;17:55-19:05" to pairs of times.
     */
    private static List<TimeRange> parseTariffRanges(String str) {
        List<TimeRange> result = new ArrayList<>();
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm");
        for (String interval : str.split(";")) {
            String[] times = interval.split("-");
            Validate.isTrue(times.length == 2, "Invalid time interval '%s' of string %s", interval, str);
            result.add(new TimeRange(
                    LocalTime.parse(times[0], formatter),
                    LocalTime.parse(times[1], formatter)
            ));
        }
        return result;
    }

    abstract public void onIntervalStart();

    abstract public void onIntervalEnd();
}
