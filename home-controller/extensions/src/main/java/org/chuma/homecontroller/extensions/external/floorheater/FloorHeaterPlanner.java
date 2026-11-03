package org.chuma.homecontroller.extensions.external.floorheater;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chuma.homecontroller.extensions.external.inverter.ElectricitySpotPriceMonitor.IntervalPrice;

/**
 * Chooses the cheapest 15-minute slots before a target time for the floor heating. Pure functions, no I/O.
 */
public final class FloorHeaterPlanner {
    public static final int SLOT_MINUTES = 15;
    private static final long SLOT_MILLIS = SLOT_MINUTES * 60_000L;

    private FloorHeaterPlanner() {
    }

    /**
     * The next target time after now; the one a day later when fewer than {@code minutes} remain until it.
     */
    public static LocalDateTime nextDeadline(LocalDateTime now, LocalTime target, int minutes) {
        LocalDateTime deadline = now.toLocalDate().atTime(target);
        if (!deadline.isAfter(now)) {
            deadline = deadline.plusDays(1);
        }
        if (Duration.between(nextSlotStart(now), deadline).toMinutes() < minutes) {
            deadline = deadline.plusDays(1);
        }
        return deadline;
    }

    /**
     * Picks {@code minutes / 15} slots between now and the deadline with the lowest compared price, which is the
     * price plus {@code bonusPerHour} for every hour between the end of the slot and the deadline, so that a later
     * slot wins over an equally priced earlier one and the heat is not lost before the morning. Ties go to the
     * later slot. Adjacent slots are merged into one range.
     *
     * @return planned ranges, or null when a price of some slot of the window is not known
     */
    public static List<HeatingRange> plan(IntervalPrice[] prices, LocalDateTime now, LocalDateTime deadline,
                                          int minutes, double bonusPerHour) {
        ZoneId zone = ZoneId.systemDefault();
        Map<Long, Double> priceByTime = new HashMap<>();
        if (prices != null) {
            for (IntervalPrice p : prices) {
                priceByTime.put(p.time(), p.price());
            }
        }
        long deadlineMillis = toMillis(deadline, zone);
        List<Slot> slots = new ArrayList<>();
        for (long t = toMillis(nextSlotStart(now), zone); t < deadlineMillis; t += SLOT_MILLIS) {
            Double price = priceByTime.get(t);
            if (price == null) {
                return null;
            }
            double hoursToDeadline = (deadlineMillis - (t + SLOT_MILLIS)) / 3_600_000.0;
            slots.add(new Slot(t, price + bonusPerHour * hoursToDeadline));
        }
        slots.sort(Comparator.comparingDouble(Slot::compared).thenComparing(Comparator.comparingLong(Slot::time).reversed()));
        List<Slot> chosen = new ArrayList<>(slots.subList(0, Math.min(slots.size(), Math.round(minutes / (float)SLOT_MINUTES))));
        chosen.sort(Comparator.comparingLong(Slot::time));

        List<HeatingRange> result = new ArrayList<>();
        for (Slot s : chosen) {
            LocalDateTime from = toDateTime(s.time, zone);
            LocalDateTime to = toDateTime(s.time + SLOT_MILLIS, zone);
            HeatingRange last = result.isEmpty() ? null : result.get(result.size() - 1);
            if (last != null && last.to().equals(from)) {
                result.set(result.size() - 1, new HeatingRange(last.from(), to, HeatingRange.Kind.PLANNED));
            } else {
                result.add(new HeatingRange(from, to, HeatingRange.Kind.PLANNED));
            }
        }
        return result;
    }

    /**
     * Start of the first slot not yet started: now rounded up to a 15-minute boundary.
     */
    public static LocalDateTime nextSlotStart(LocalDateTime now) {
        LocalDateTime minute = now.withSecond(0).withNano(0);
        int over = minute.getMinute() % SLOT_MINUTES;
        if (over == 0 && minute.equals(now)) {
            return minute;
        }
        return minute.plusMinutes(SLOT_MINUTES - over);
    }

    private static long toMillis(LocalDateTime time, ZoneId zone) {
        return time.atZone(zone).toInstant().toEpochMilli();
    }

    private static LocalDateTime toDateTime(long millis, ZoneId zone) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone);
    }

    private record Slot(long time, double compared) {
    }
}
