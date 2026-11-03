package org.chuma.homecontroller.extensions.external.floorheater;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.controller.actor.IOnOffActor;
import org.chuma.homecontroller.controller.persistence.StateMap;
import org.chuma.homecontroller.extensions.external.floorheater.HeatingRange.Kind;
import org.chuma.homecontroller.extensions.external.inverter.ElectricitySpotPriceMonitor.IntervalPrice;

/**
 * Drives the electric floor heating relays from {@link HeatingRange}s: a relay is on exactly while the current
 * time is inside one of the ranges of its room. The ranges come from the {@link FloorHeaterPlanner} (one-shot
 * morning schedule), from the on/off switch (a manual range of {@link #MANUAL_MINUTES}) or from the user editing
 * them. Everything is kept in the {@link StateMap}, so it survives a restart. A tick every minute applies the
 * ranges and removes the ones that ended; it is idempotent, so a relay changed elsewhere is corrected as well.
 */
public class FloorHeaterManager {
    static Logger log = LoggerFactory.getLogger(FloorHeaterManager.class.getName());

    public static final int MANUAL_MINUTES = 6 * 60;
    static final int DEFAULT_MINUTES = 3 * 60;
    static final int DEFAULT_TARGET_MINUTES = 10 * 60;
    static final int DEFAULT_LATER_BONUS_HALER = 20;
    static final int MAX_MINUTES = 8 * 60;

    private static final String KEY_PREFIX = "floorHeater.";
    private static final String KEY_TARGET = KEY_PREFIX + "targetMinutes";
    private static final String KEY_LATER_BONUS = KEY_PREFIX + "laterBonusHaler";

    public record Room(String id, String name, int powerW, IOnOffActor actor) {
    }

    /**
     * @param running the range containing now, null when the heater is off
     * @param scheduleUntil deadline of the one-shot morning schedule, null when it is off
     * @param canPlan whether the prices cover the planning window of the room
     */
    public record Status(Room room, boolean on, List<HeatingRange> ranges, HeatingRange running, int minutes,
                         LocalDateTime scheduleUntil, boolean canPlan) {
    }

    private final Map<String, Room> rooms = new LinkedHashMap<>();
    private final Supplier<IntervalPrice[]> prices;
    private final StateMap stateMap;
    private final Clock clock;
    private ScheduledExecutorService executor;

    /**
     * @param prices supplies the known 15-minute prices, null when they are not available
     */
    public FloorHeaterManager(List<Room> rooms, Supplier<IntervalPrice[]> prices, StateMap stateMap, Clock clock) {
        for (Room r : rooms) {
            Validate.isTrue(this.rooms.put(r.id(), r) == null, "Duplicate floor heater id '%s'", r.id());
        }
        this.prices = prices;
        this.stateMap = stateMap;
        this.clock = clock;
    }

    /**
     * Applies the ranges now and then every minute, right after the minute starts.
     */
    public synchronized void start() {
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "FloorHeaterManager"));
        long toNextMinute = 60 - (clock.millis() / 1000) % 60 + 1;
        executor.scheduleAtFixedRate(this::safeTick, toNextMinute, 60, TimeUnit.SECONDS);
        safeTick();
    }

    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void safeTick() {
        try {
            tick();
        } catch (Exception e) {
            log.error("Floor heating tick failed", e);
        }
    }

    public List<Room> getRooms() {
        return new ArrayList<>(rooms.values());
    }

    public int getTargetMinutes() {
        return intValue(KEY_TARGET, DEFAULT_TARGET_MINUTES);
    }

    public int getLaterBonusHaler() {
        return intValue(KEY_LATER_BONUS, DEFAULT_LATER_BONUS_HALER);
    }

    public int getMinutes(String id) {
        return intValue(minutesKey(room(id)), DEFAULT_MINUTES);
    }

    public List<HeatingRange> getRanges(String id) {
        return HeatingRange.parse(stateMap.getString(timesKey(room(id))));
    }

    public LocalDateTime getScheduleUntil(String id) {
        String s = stateMap.getString(scheduleKey(room(id)));
        return s != null ? LocalDateTime.parse(s) : null;
    }

    public synchronized Status getStatus(String id) {
        return status(room(id), now(), prices.get());
    }

    public synchronized List<Status> getStatuses() {
        LocalDateTime now = now();
        IntervalPrice[] p = prices.get();
        List<Status> result = new ArrayList<>();
        for (Room r : rooms.values()) {
            result.add(status(r, now, p));
        }
        return result;
    }

    /**
     * Removes the ranges that ended and the finished schedule, then sets every relay to the state its ranges say.
     */
    public synchronized void tick() {
        LocalDateTime now = now();
        for (Room r : rooms.values()) {
            List<HeatingRange> ranges = ranges(r);
            List<HeatingRange> kept = ranges.stream().filter(x -> x.to().isAfter(now)).toList();
            if (kept.size() != ranges.size()) {
                storeRanges(r, kept);
            }
            LocalDateTime until = scheduleUntil(r);
            if (until != null && !now.isBefore(until)) {
                stateMap.removeValue(scheduleKey(r));
                log.info("Floor heating {}: morning schedule until {} finished", r.name(), until);
            }
            apply(r, kept, now);
        }
    }

    private void apply(Room r, List<HeatingRange> ranges, LocalDateTime now) {
        HeatingRange running = running(ranges, now);
        boolean desired = running != null;
        if (r.actor().isOn() != desired) {
            log.info("Floor heating {}: {} {}", r.name(), desired ? "ON until " + running.to() : "OFF",
                    desired ? "(" + running.kind().name().toLowerCase() + " range from " + running.from() + ")" : "");
            if (desired) {
                r.actor().switchOn();
            } else {
                r.actor().switchOff();
            }
        }
    }

    /**
     * Adds a manual range from now for {@link #MANUAL_MINUTES}; nothing happens while a range is running.
     */
    public synchronized void switchOn(String id) {
        Room r = room(id);
        LocalDateTime now = now();
        List<HeatingRange> ranges = ranges(r);
        if (running(ranges, now) != null) {
            return;
        }
        LocalDateTime from = now.truncatedTo(ChronoUnit.MINUTES);
        ranges.add(new HeatingRange(from, from.plusMinutes(MANUAL_MINUTES), Kind.MANUAL));
        storeRanges(r, HeatingRange.normalize(ranges));
        apply(r, ranges(r), now);
    }

    /**
     * Ends the running range now; nothing happens while no range is running.
     */
    public synchronized void switchOff(String id) {
        Room r = room(id);
        LocalDateTime now = now();
        List<HeatingRange> ranges = ranges(r);
        HeatingRange running = running(ranges, now);
        if (running == null) {
            return;
        }
        LocalDateTime end = now.truncatedTo(ChronoUnit.MINUTES);
        ranges.remove(running);
        if (end.isAfter(running.from())) {
            ranges.add(new HeatingRange(running.from(), end, running.kind()));
        }
        storeRanges(r, HeatingRange.normalize(ranges));
        apply(r, ranges(r), now);
    }

    /**
     * Turns the one-shot morning schedule on or off. On: the planned ranges are replaced by a new plan up to the
     * next target time. Off: the planned ranges are removed, manual ones stay.
     *
     * @throws IllegalStateException when the prices needed for the plan are not published yet
     */
    public synchronized void setSchedule(String id, boolean on) {
        Room r = room(id);
        if (on) {
            Plan plan = planRoom(r, now(), prices.get(), getMinutes(id), getTargetMinutes(), getLaterBonusHaler());
            storePlan(r, plan);
        } else {
            storeRanges(r, withoutPlanned(ranges(r)));
            stateMap.removeValue(scheduleKey(r));
        }
        apply(r, ranges(r), now());
    }

    public synchronized void setMinutes(String id, int minutes) {
        Validate.inclusiveBetween(FloorHeaterPlanner.SLOT_MINUTES, MAX_MINUTES, minutes);
        Room r = room(id);
        Plan plan = scheduleUntil(r) != null
                ? planRoom(r, now(), prices.get(), minutes, getTargetMinutes(), getLaterBonusHaler()) : null;
        stateMap.setValue(minutesKey(r), minutes);
        if (plan != null) {
            storePlan(r, plan);
            apply(r, ranges(r), now());
        }
    }

    public synchronized void setTargetMinutes(int minutesOfDay) {
        Validate.inclusiveBetween(0, 24 * 60 - 1, minutesOfDay);
        replanAll(minutesOfDay, getLaterBonusHaler());
        stateMap.setValue(KEY_TARGET, minutesOfDay);
    }

    public synchronized void setLaterBonusHaler(int haler) {
        Validate.inclusiveBetween(0, 1000, haler);
        replanAll(getTargetMinutes(), haler);
        stateMap.setValue(KEY_LATER_BONUS, haler);
    }

    /**
     * Replaces the ranges of a room with an edited list.
     *
     * @throws IllegalArgumentException when the list is malformed or invalid
     */
    public synchronized void setRanges(String id, String times) {
        Room r = room(id);
        List<HeatingRange> ranges = HeatingRange.parse(times);
        String error = HeatingRange.validate(ranges);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        storeRanges(r, HeatingRange.normalize(ranges));
        apply(r, ranges(r), now());
    }

    /**
     * Plans every room whose schedule is on with the new settings; nothing is stored when any plan fails.
     */
    private void replanAll(int targetMinutes, int bonusHaler) {
        LocalDateTime now = now();
        IntervalPrice[] p = prices.get();
        Map<Room, Plan> plans = new LinkedHashMap<>();
        for (Room r : rooms.values()) {
            if (scheduleUntil(r) != null) {
                plans.put(r, planRoom(r, now, p, getMinutes(r.id()), targetMinutes, bonusHaler));
            }
        }
        plans.forEach((r, plan) -> {
            storePlan(r, plan);
            apply(r, ranges(r), now);
        });
    }

    private record Plan(List<HeatingRange> ranges, LocalDateTime deadline) {
    }

    private Plan planRoom(Room r, LocalDateTime now, IntervalPrice[] p, int minutes, int targetMinutes, int bonusHaler) {
        LocalDateTime deadline = FloorHeaterPlanner.nextDeadline(now, LocalTime.ofSecondOfDay(targetMinutes * 60L), minutes);
        List<HeatingRange> planned = FloorHeaterPlanner.plan(p, now, deadline, minutes, bonusHaler / 100.0);
        if (planned == null) {
            throw new IllegalStateException("Prices until " + deadline.toLocalDate() + " " + deadline.toLocalTime()
                    + " are not published yet");
        }
        List<HeatingRange> ranges = withoutPlanned(ranges(r));
        ranges.addAll(planned);
        return new Plan(HeatingRange.normalize(ranges), deadline);
    }

    private void storePlan(Room r, Plan plan) {
        storeRanges(r, plan.ranges());
        stateMap.setValue(scheduleKey(r), plan.deadline().toString());
        log.info("Floor heating {}: planned {} until {}", r.name(),
                HeatingRange.format(plan.ranges().stream().filter(x -> x.kind() == Kind.PLANNED).toList()), plan.deadline());
    }

    private boolean canPlan(Room r, LocalDateTime now, IntervalPrice[] p) {
        int minutes = getMinutes(r.id());
        LocalDateTime deadline = FloorHeaterPlanner.nextDeadline(now, LocalTime.ofSecondOfDay(getTargetMinutes() * 60L), minutes);
        return FloorHeaterPlanner.plan(p, now, deadline, minutes, getLaterBonusHaler() / 100.0) != null;
    }

    private Status status(Room r, LocalDateTime now, IntervalPrice[] p) {
        List<HeatingRange> ranges = ranges(r);
        return new Status(r, r.actor().isOn(), ranges, running(ranges, now), getMinutes(r.id()), scheduleUntil(r), canPlan(r, now, p));
    }

    private static HeatingRange running(List<HeatingRange> ranges, LocalDateTime now) {
        return ranges.stream().filter(x -> x.contains(now)).findFirst().orElse(null);
    }

    private static List<HeatingRange> withoutPlanned(List<HeatingRange> ranges) {
        return new ArrayList<>(ranges.stream().filter(x -> x.kind() != Kind.PLANNED).toList());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private Room room(String id) {
        Room r = rooms.get(id);
        Validate.notNull(r, "Unknown floor heater '%s'", id);
        return r;
    }

    private List<HeatingRange> ranges(Room r) {
        return HeatingRange.parse(stateMap.getString(timesKey(r)));
    }

    private void storeRanges(Room r, List<HeatingRange> ranges) {
        if (ranges.isEmpty()) {
            stateMap.removeValue(timesKey(r));
        } else {
            stateMap.setValue(timesKey(r), HeatingRange.format(ranges));
        }
    }

    private LocalDateTime scheduleUntil(Room r) {
        String s = stateMap.getString(scheduleKey(r));
        return s != null ? LocalDateTime.parse(s) : null;
    }

    private int intValue(String key, int defaultValue) {
        Integer v = stateMap.getValue(key);
        return v != null ? v : defaultValue;
    }

    private static String timesKey(Room r) {
        return KEY_PREFIX + r.id() + ".times";
    }

    private static String minutesKey(Room r) {
        return KEY_PREFIX + r.id() + ".minutes";
    }

    private static String scheduleKey(Room r) {
        return KEY_PREFIX + r.id() + ".scheduleUntil";
    }
}
