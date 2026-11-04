package org.chuma.homecontroller.extensions.external.floorheater;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import org.chuma.homecontroller.extensions.external.inverter.ElectricitySpotPriceMonitor.IntervalPrice;

public class FloorHeaterPlannerTest {
    private static final LocalTime TARGET = LocalTime.of(10, 0);

    private static LocalDateTime t(String s) {
        return LocalDateTime.parse(s);
    }

    /**
     * 15-minute prices of the given days, the price of a slot given by its local time.
     */
    private static IntervalPrice[] prices(Function<LocalDateTime, Double> price, LocalDate... days) {
        List<IntervalPrice> result = new ArrayList<>();
        for (LocalDate day : days) {
            for (LocalDateTime t = day.atStartOfDay(); t.toLocalDate().equals(day); t = t.plusMinutes(15)) {
                long millis = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                result.add(new IntervalPrice(millis, price.apply(t), 1.71, 0.54));
            }
        }
        return result.toArray(new IntervalPrice[0]);
    }

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);

    @Test
    public void testNextDeadline() {
        Assert.assertEquals(t("2026-11-04T10:00"), FloorHeaterPlanner.nextDeadline(t("2026-11-03T20:00"), TARGET));
        Assert.assertEquals(t("2026-11-03T10:00"), FloorHeaterPlanner.nextDeadline(t("2026-11-03T02:00"), TARGET));
        // the nearest target counts even when the duration does not fit any more
        Assert.assertEquals(t("2026-11-03T10:00"), FloorHeaterPlanner.nextDeadline(t("2026-11-03T08:00"), TARGET));
        Assert.assertEquals(t("2026-11-03T10:00"), FloorHeaterPlanner.nextDeadline(t("2026-11-03T09:45"), TARGET));
        // 09:46 -> first slot 10:00, no whole slot left before the target
        Assert.assertEquals(t("2026-11-04T10:00"), FloorHeaterPlanner.nextDeadline(t("2026-11-03T09:46"), TARGET));
        Assert.assertEquals(t("2026-11-04T10:00"), FloorHeaterPlanner.nextDeadline(t("2026-11-03T10:00"), TARGET));
    }

    @Test
    public void testNextSlotStart() {
        Assert.assertEquals(t("2026-11-03T20:00"), FloorHeaterPlanner.nextSlotStart(t("2026-11-03T20:00")));
        Assert.assertEquals(t("2026-11-03T20:15"), FloorHeaterPlanner.nextSlotStart(t("2026-11-03T20:00:01")));
        Assert.assertEquals(t("2026-11-03T20:15"), FloorHeaterPlanner.nextSlotStart(t("2026-11-03T20:14:59")));
        Assert.assertEquals(t("2026-11-04T00:00"), FloorHeaterPlanner.nextSlotStart(t("2026-11-03T23:50")));
    }

    @Test
    public void testMissingPricesGiveNoPlan() {
        IntervalPrice[] todayOnly = prices(t -> 10.0, TODAY);
        Assert.assertNull(FloorHeaterPlanner.plan(todayOnly, t("2026-11-03T20:00"), t("2026-11-04T10:00"), 180, 0.2));
        Assert.assertNull(FloorHeaterPlanner.plan(null, t("2026-11-03T20:00"), t("2026-11-04T10:00"), 180, 0.2));
        // today's prices are enough for a window ending today
        Assert.assertNotNull(FloorHeaterPlanner.plan(todayOnly, t("2026-11-03T02:00"), t("2026-11-03T10:00"), 180, 0.2));
    }

    @Test
    public void testCheapestSlotsAreMerged() {
        // three cheap slots 2:00-2:45 tomorrow, everything else expensive
        IntervalPrice[] p = prices(t -> t.toLocalDate().equals(TOMORROW) && !t.toLocalTime().isBefore(LocalTime.of(2, 0))
                && t.toLocalTime().isBefore(LocalTime.of(2, 45)) ? 5.0 : 10.0, TODAY, TOMORROW);
        List<HeatingRange> plan = FloorHeaterPlanner.plan(p, t("2026-11-03T20:00"), t("2026-11-04T10:00"), 45, 0.0);
        Assert.assertEquals("P2026-11-04T02:00/2026-11-04T02:45", HeatingRange.format(plan));
    }

    @Test
    public void testTieGoesToTheLaterSlot() {
        IntervalPrice[] p = prices(t -> 10.0, TODAY, TOMORROW);
        List<HeatingRange> plan = FloorHeaterPlanner.plan(p, t("2026-11-03T20:00"), t("2026-11-04T10:00"), 30, 0.0);
        Assert.assertEquals("P2026-11-04T09:30/2026-11-04T10:00", HeatingRange.format(plan));
    }

    @Test
    public void testBonusPrefersTheMorning() {
        // night 1:00-2:00 at 8.00, morning 7:00-8:00 at 8.50, the rest 12.00
        Function<LocalDateTime, Double> f = t -> {
            if (!t.toLocalDate().equals(TOMORROW)) return 12.0;
            int h = t.getHour();
            return h == 1 ? 8.0 : h == 7 ? 8.5 : 12.0;
        };
        IntervalPrice[] p = prices(f, TODAY, TOMORROW);
        Assert.assertEquals("P2026-11-04T01:00/2026-11-04T02:00",
                HeatingRange.format(FloorHeaterPlanner.plan(p, t("2026-11-03T20:00"), t("2026-11-04T10:00"), 60, 0.0)));
        // 1:00 slot ends 1:15, 8.75 h before 10:00: 8.00 + 0.2 * 8.75 = 9.75; 7:00 slot: 8.50 + 0.2 * 2.75 = 9.05
        Assert.assertEquals("P2026-11-04T07:00/2026-11-04T08:00",
                HeatingRange.format(FloorHeaterPlanner.plan(p, t("2026-11-03T20:00"), t("2026-11-04T10:00"), 60, 0.2)));
    }

    @Test
    public void testWindowStartsAtTheNextSlot() {
        // the cheapest slot is the one already running, it must not be chosen
        IntervalPrice[] p = prices(t -> t.equals(t("2026-11-03T20:00")) ? 1.0 : t.equals(t("2026-11-03T20:15")) ? 2.0 : 10.0, TODAY, TOMORROW);
        List<HeatingRange> plan = FloorHeaterPlanner.plan(p, t("2026-11-03T20:05"), t("2026-11-04T10:00"), 15, 0.0);
        Assert.assertEquals("P2026-11-03T20:15/2026-11-03T20:30", HeatingRange.format(plan));
    }

    @Test
    public void testShortWindowTakesAllSlots() {
        IntervalPrice[] p = prices(t -> 10.0, TODAY);
        List<HeatingRange> plan = FloorHeaterPlanner.plan(p, t("2026-11-03T09:00"), t("2026-11-03T10:00"), 180, 0.2);
        Assert.assertEquals("P2026-11-03T09:00/2026-11-03T10:00", HeatingRange.format(plan));
    }

    @Test
    public void testDurationIsRoundedToSlots() {
        IntervalPrice[] p = prices(t -> 10.0, TODAY, TOMORROW);
        List<HeatingRange> plan = FloorHeaterPlanner.plan(p, t("2026-11-03T20:00"), t("2026-11-04T10:00"), 50, 0.0);
        Assert.assertEquals("P2026-11-04T09:15/2026-11-04T10:00", HeatingRange.format(plan));
    }
}
