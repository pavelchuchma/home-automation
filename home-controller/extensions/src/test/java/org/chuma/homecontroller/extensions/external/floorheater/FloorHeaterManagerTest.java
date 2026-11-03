package org.chuma.homecontroller.extensions.external.floorheater;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.chuma.homecontroller.controller.actor.IOnOffActor;
import org.chuma.homecontroller.controller.persistence.StateMap;
import org.chuma.homecontroller.extensions.external.floorheater.FloorHeaterManager.Room;
import org.chuma.homecontroller.extensions.external.floorheater.FloorHeaterManager.Status;
import org.chuma.homecontroller.extensions.external.inverter.ElectricitySpotPriceMonitor.IntervalPrice;

public class FloorHeaterManagerTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);

    static class FakeActor implements IOnOffActor {
        final String id;
        boolean on;
        int switches;

        FakeActor(String id) {
            this.id = id;
        }

        @Override
        public boolean switchOn(Object actionData) {
            on = true;
            switches++;
            return true;
        }

        @Override
        public boolean switchOff(Object actionData) {
            on = false;
            switches++;
            return true;
        }

        @Override
        public boolean isOn() {
            return on;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getLabel() {
            return id;
        }

        @Override
        public Object getActionData() {
            return null;
        }

        @Override
        public void setActionData(Object actionData) {
        }

        @Override
        public void callListenersAndSetActionData(Object actionData) {
        }
    }

    static class MemoryStateMap implements StateMap {
        final Map<String, String> map = new HashMap<>();

        @Override
        public void setValue(String key, int value) {
            map.put(key, Integer.toString(value));
        }

        @Override
        public void setValue(String key, String value) {
            map.put(key, value);
        }

        @Override
        public void removeValue(String key) {
            map.remove(key);
        }

        @Override
        public Integer getValue(String key) {
            return map.containsKey(key) ? Integer.valueOf(map.get(key)) : null;
        }

        @Override
        public String getString(String key) {
            return map.get(key);
        }
    }

    static class MutableClock extends Clock {
        Instant instant;

        MutableClock(LocalDateTime time) {
            set(time);
        }

        void set(LocalDateTime time) {
            instant = time.atZone(ZoneId.systemDefault()).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private static LocalDateTime t(String s) {
        return LocalDateTime.parse(s);
    }

    /** Flat 10 CZK/kWh with a cheap hour 3:00-4:00 on every given day. */
    private static IntervalPrice[] prices(LocalDate... days) {
        List<IntervalPrice> result = new ArrayList<>();
        for (LocalDate day : days) {
            for (LocalDateTime t = day.atStartOfDay(); t.toLocalDate().equals(day); t = t.plusMinutes(15)) {
                long millis = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                result.add(new IntervalPrice(millis, t.getHour() == 3 ? 5.0 : 10.0, 1.71, 0.54));
            }
        }
        return result.toArray(new IntervalPrice[0]);
    }

    private final FakeActor bath = new FakeActor("pdlKoupH");
    private final FakeActor bedroom = new FakeActor("pdlLoz");
    private final MemoryStateMap state = new MemoryStateMap();
    private final MutableClock clock = new MutableClock(t("2026-11-03T20:00:00"));
    private IntervalPrice[] prices = prices(TODAY, TODAY.plusDays(1));
    private FloorHeaterManager manager;

    @Before
    public void setUp() {
        manager = new FloorHeaterManager(List.of(
                new Room("pdlKoupH", "Koupelna hore", 550, bath),
                new Room("pdlLoz", "Loznice", 450, bedroom)), () -> prices, state, clock);
        // the cheap hour of the test prices
        manager.setMinutes("pdlKoupH", 60);
    }

    @Test
    public void testSwitchOnMakesManualRangeAndSwitchOffEndsIt() {
        clock.set(t("2026-11-03T20:00:30"));
        manager.switchOn("pdlKoupH");
        Assert.assertTrue(bath.on);
        Assert.assertEquals("M2026-11-03T20:00/2026-11-04T02:00", state.getString("floorHeater.pdlKoupH.times"));
        Status s = manager.getStatus("pdlKoupH");
        Assert.assertTrue(s.on());
        Assert.assertEquals(t("2026-11-04T02:00"), s.running().to());

        // a second switch-on changes nothing
        manager.switchOn("pdlKoupH");
        Assert.assertEquals(1, bath.switches);

        clock.set(t("2026-11-03T21:45:10"));
        manager.switchOff("pdlKoupH");
        Assert.assertFalse(bath.on);
        Assert.assertEquals("M2026-11-03T20:00/2026-11-03T21:45", state.getString("floorHeater.pdlKoupH.times"));
        Assert.assertFalse(bedroom.on);

        // the ended range goes away with the next tick; switching off right after switching on removes the range
        manager.tick();
        manager.switchOn("pdlKoupH");
        manager.switchOff("pdlKoupH");
        Assert.assertNull(state.getString("floorHeater.pdlKoupH.times"));
    }

    @Test
    public void testTickFollowsTheRangesAndDropsEndedOnes() {
        manager.setRanges("pdlKoupH", "P2026-11-04T03:00/2026-11-04T04:00;P2026-11-04T05:00/2026-11-04T05:15");
        manager.tick();
        Assert.assertFalse(bath.on);

        clock.set(t("2026-11-04T03:00:00"));
        manager.tick();
        Assert.assertTrue(bath.on);

        clock.set(t("2026-11-04T03:59:59"));
        manager.tick();
        Assert.assertTrue(bath.on);

        clock.set(t("2026-11-04T04:00:00"));
        manager.tick();
        Assert.assertFalse(bath.on);
        Assert.assertEquals("P2026-11-04T05:00/2026-11-04T05:15", state.getString("floorHeater.pdlKoupH.times"));

        clock.set(t("2026-11-04T05:20:00"));
        manager.tick();
        Assert.assertFalse(bath.on);
        Assert.assertNull(state.getString("floorHeater.pdlKoupH.times"));
    }

    @Test
    public void testTickRevertsRelayChangedElsewhere() {
        manager.setRanges("pdlKoupH", "M2026-11-03T19:00/2026-11-03T22:00");
        Assert.assertTrue(bath.on);
        bath.on = false;
        manager.tick();
        Assert.assertTrue(bath.on);

        bedroom.on = true;
        manager.tick();
        Assert.assertFalse(bedroom.on);
    }

    @Test
    public void testSchedulePlansTheCheapHourAndIsOneShot() {
        manager.setSchedule("pdlKoupH", true);
        Assert.assertEquals("P2026-11-04T03:00/2026-11-04T04:00", state.getString("floorHeater.pdlKoupH.times"));
        Assert.assertEquals("2026-11-04T10:00", state.getString("floorHeater.pdlKoupH.scheduleUntil"));
        Assert.assertEquals(t("2026-11-04T10:00"), manager.getStatus("pdlKoupH").scheduleUntil());
        Assert.assertFalse(bath.on);

        clock.set(t("2026-11-04T03:30:00"));
        manager.tick();
        Assert.assertTrue(bath.on);

        clock.set(t("2026-11-04T10:00:00"));
        manager.tick();
        Assert.assertFalse(bath.on);
        Assert.assertNull(state.getString("floorHeater.pdlKoupH.scheduleUntil"));
        Assert.assertNull(state.getString("floorHeater.pdlKoupH.times"));
        Assert.assertNull(manager.getStatus("pdlKoupH").scheduleUntil());
    }

    @Test
    public void testScheduleOffKeepsManualRanges() {
        manager.setRanges("pdlKoupH", "M2026-11-03T22:00/2026-11-03T23:00");
        manager.setSchedule("pdlKoupH", true);
        Assert.assertEquals("M2026-11-03T22:00/2026-11-03T23:00;P2026-11-04T03:00/2026-11-04T04:00", state.getString("floorHeater.pdlKoupH.times"));

        // enabling again replaces the planned ranges only
        manager.setRanges("pdlKoupH", "M2026-11-03T22:00/2026-11-03T23:00;P2026-11-04T06:00/2026-11-04T07:00");
        manager.setSchedule("pdlKoupH", true);
        Assert.assertEquals("M2026-11-03T22:00/2026-11-03T23:00;P2026-11-04T03:00/2026-11-04T04:00", state.getString("floorHeater.pdlKoupH.times"));

        manager.setSchedule("pdlKoupH", false);
        Assert.assertEquals("M2026-11-03T22:00/2026-11-03T23:00", state.getString("floorHeater.pdlKoupH.times"));
        Assert.assertNull(state.getString("floorHeater.pdlKoupH.scheduleUntil"));
    }

    @Test
    public void testScheduleRefusedWithoutPrices() {
        prices = prices(TODAY);
        Assert.assertFalse(manager.getStatus("pdlKoupH").canPlan());
        try {
            manager.setSchedule("pdlKoupH", true);
            Assert.fail("expected an exception");
        } catch (IllegalStateException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("not published"));
        }
        Assert.assertNull(state.getString("floorHeater.pdlKoupH.times"));
        Assert.assertNull(state.getString("floorHeater.pdlKoupH.scheduleUntil"));

        prices = prices(TODAY, TODAY.plusDays(1));
        Assert.assertTrue(manager.getStatus("pdlKoupH").canPlan());
    }

    @Test
    public void testChangedMinutesReplanOnlyWithScheduleOn() {
        manager.setMinutes("pdlKoupH", 30);
        Assert.assertEquals(30, manager.getMinutes("pdlKoupH"));
        Assert.assertNull(state.getString("floorHeater.pdlKoupH.times"));

        manager.setSchedule("pdlKoupH", true);
        // ties go to the later slot, so the last 30 minutes of the cheap hour
        Assert.assertEquals("P2026-11-04T03:30/2026-11-04T04:00", state.getString("floorHeater.pdlKoupH.times"));

        manager.setMinutes("pdlKoupH", 60);
        Assert.assertEquals("P2026-11-04T03:00/2026-11-04T04:00", state.getString("floorHeater.pdlKoupH.times"));
        Assert.assertEquals(60, manager.getStatus("pdlKoupH").minutes());
    }

    @Test
    public void testSharedSettingsReplanAllScheduledRooms() {
        manager.setMinutes("pdlKoupH", 30);
        manager.setSchedule("pdlKoupH", true);
        manager.setMinutes("pdlLoz", 15);
        Assert.assertEquals(FloorHeaterManager.DEFAULT_TARGET_MINUTES, manager.getTargetMinutes());

        // target 3:30 cuts the cheap hour in half for the scheduled room, the other room is untouched
        manager.setTargetMinutes(3 * 60 + 30);
        Assert.assertEquals("P2026-11-04T03:00/2026-11-04T03:30", state.getString("floorHeater.pdlKoupH.times"));
        Assert.assertEquals("2026-11-04T03:30", state.getString("floorHeater.pdlKoupH.scheduleUntil"));
        Assert.assertNull(state.getString("floorHeater.pdlLoz.times"));
        Assert.assertEquals(210, manager.getTargetMinutes());

        manager.setLaterBonusHaler(50);
        Assert.assertEquals(50, manager.getLaterBonusHaler());
    }

    @Test
    public void testSharedSettingNotStoredWhenReplanFails() {
        manager.setSchedule("pdlKoupH", true);
        prices = prices(TODAY);
        try {
            manager.setTargetMinutes(11 * 60);
            Assert.fail("expected an exception");
        } catch (IllegalStateException e) {
            // expected
        }
        Assert.assertEquals(FloorHeaterManager.DEFAULT_TARGET_MINUTES, manager.getTargetMinutes());
        Assert.assertEquals("P2026-11-04T03:00/2026-11-04T04:00", state.getString("floorHeater.pdlKoupH.times"));
    }

    @Test
    public void testSetRangesValidates() {
        try {
            manager.setRanges("pdlKoupH", "P2026-11-04T03:00/2026-11-04T03:00");
            Assert.fail("expected an exception");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Range 1 must end after it starts.", e.getMessage());
        }
        try {
            manager.setRanges("pdlKoupH", "P2026-11-04T03:00/2026-11-04T04:00;M2026-11-04T03:30/2026-11-04T05:00");
            Assert.fail("expected an exception");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("Ranges 1 and 2 overlap"));
        }
        try {
            manager.setRanges("pdlXXX", "");
            Assert.fail("expected an exception");
        } catch (NullPointerException | IllegalArgumentException e) {
            // unknown room
        }
        Assert.assertNull(state.getString("floorHeater.pdlKoupH.times"));

        // touching ranges are merged
        manager.setRanges("pdlKoupH", "P2026-11-04T03:00/2026-11-04T04:00;P2026-11-04T04:00/2026-11-04T04:30");
        Assert.assertEquals("P2026-11-04T03:00/2026-11-04T04:30", state.getString("floorHeater.pdlKoupH.times"));
    }

    @Test
    public void testRestartAppliesStoredState() {
        manager.switchOn("pdlKoupH");
        bath.on = false;
        FloorHeaterManager restarted = new FloorHeaterManager(List.of(
                new Room("pdlKoupH", "Koupelna hore", 550, bath),
                new Room("pdlLoz", "Loznice", 450, bedroom)), () -> prices, state, clock);
        restarted.tick();
        Assert.assertTrue(bath.on);
        Assert.assertEquals(2, restarted.getStatuses().size());
        Assert.assertTrue(restarted.getStatuses().get(0).on());
        Assert.assertFalse(restarted.getStatuses().get(1).on());
    }
}
