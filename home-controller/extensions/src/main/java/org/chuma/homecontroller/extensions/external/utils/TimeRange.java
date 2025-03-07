package org.chuma.homecontroller.extensions.external.utils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import org.chuma.homecontroller.extensions.external.inverter.InverterManager;

public class TimeRange {
    public final LocalTime from;
    public final LocalTime to;

    TimeRange(LocalTime from, LocalTime to) {
        this.from = from;
        this.to = to;
    }

    /**
     * Parses range string like "6:55-8:05;17:55-19:05" to pairs of times.
     */
    public static List<TimeRange> parseTariffRanges(String str) {
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
}
