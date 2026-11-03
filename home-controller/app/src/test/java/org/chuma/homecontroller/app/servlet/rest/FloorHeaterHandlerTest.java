package org.chuma.homecontroller.app.servlet.rest;

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

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.controller.actor.IOnOffActor;
import org.chuma.homecontroller.controller.persistence.StateMap;
import org.chuma.homecontroller.extensions.external.floorheater.FloorHeaterManager;
import org.chuma.homecontroller.extensions.external.floorheater.FloorHeaterManager.Room;
import org.chuma.homecontroller.extensions.external.inverter.ElectricitySpotPriceMonitor.IntervalPrice;

public class FloorHeaterHandlerTest {
    private static final LocalDateTime NOW = LocalDateTime.parse("2026-11-03T20:00:00");
    private static final ZoneId ZONE = ZoneId.systemDefault();

    static class FakeActor implements IOnOffActor {
        boolean on;

        @Override
        public boolean switchOn(Object actionData) {
            on = true;
            return true;
        }

        @Override
        public boolean switchOff(Object actionData) {
            on = false;
            return true;
        }

        @Override
        public boolean isOn() {
            return on;
        }

        @Override
        public String getId() {
            return "a";
        }

        @Override
        public String getLabel() {
            return "a";
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

    private static IntervalPrice[] prices(LocalDate... days) {
        List<IntervalPrice> result = new ArrayList<>();
        for (LocalDate day : days) {
            for (LocalDateTime t = day.atStartOfDay(); t.toLocalDate().equals(day); t = t.plusMinutes(15)) {
                result.add(new IntervalPrice(t.atZone(ZONE).toInstant().toEpochMilli(), t.getHour() == 3 ? 5.0 : 10.0, 1.71, 0.54));
            }
        }
        return result.toArray(new IntervalPrice[0]);
    }

    private static Map<String, String[]> params(String... keyValues) {
        Map<String, String[]> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], new String[]{keyValues[i + 1]});
        }
        return map;
    }

    private static long millis(String time) {
        return LocalDateTime.parse(time).atZone(ZONE).toInstant().toEpochMilli();
    }

    private final FakeActor actor = new FakeActor();
    private final MemoryStateMap state = new MemoryStateMap();
    private FloorHeaterManager manager;
    private FloorHeaterHandler handler;

    @Before
    public void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        IntervalPrice[] prices = prices(NOW.toLocalDate(), NOW.toLocalDate().plusDays(1));
        manager = new FloorHeaterManager(List.of(new Room("pdlKoupH", "Koupelna hore", 550, actor)), () -> prices, state, clock);
        handler = new FloorHeaterHandler(manager);
    }

    @Test
    public void statusJsonCarriesRangesAndSettings() {
        handler.processAction(handler.getItemById("pdlKoupH"), params("id", "pdlKoupH", "minutes", "60", "schedule", "on"));

        JsonWriter writer = new JsonWriter(false);
        try (JsonWriter root = writer.startObject()) {
            handler.writeStatus(root, manager.getStatus("pdlKoupH"));
        }
        String json = writer.toString();
        Assert.assertTrue(json, json.contains("\"name\":\"Koupelna hore\""));
        Assert.assertTrue(json, json.contains("\"val\":0"));
        Assert.assertTrue(json, json.contains("\"powerW\":550"));
        Assert.assertTrue(json, json.contains("\"minutes\":60"));
        Assert.assertTrue(json, json.contains("\"scheduleUntil\":" + millis("2026-11-04T10:00")));
        Assert.assertTrue(json, json.contains("\"canPlan\":true"));
        Assert.assertTrue(json, json.contains("\"target\":600"));
        Assert.assertTrue(json, json.contains("\"laterBonus\":20"));
        Assert.assertTrue(json, json.contains("\"manualMinutes\":360"));
        Assert.assertTrue(json, json.contains("\"ranges\":[{\"from\":" + millis("2026-11-04T03:00") + ",\"to\":" + millis("2026-11-04T04:00") + ",\"kind\":\"planned\"}]"));
    }

    @Test
    public void actionsAreAppliedInOrder() {
        Room room = handler.getItemById("pdlKoupH");
        handler.processAction(room, params("switch", "on"));
        Assert.assertTrue(actor.on);
        Assert.assertEquals("M2026-11-03T20:00/2026-11-04T02:00", state.getString("floorHeater.pdlKoupH.times"));

        handler.processAction(room, params("switch", "off"));
        Assert.assertFalse(actor.on);
        Assert.assertNull(state.getString("floorHeater.pdlKoupH.times"));

        handler.processAction(room, params("ranges", "M2026-11-03T22:00/2026-11-03T23:00", "target", "570", "laterBonus", "35"));
        Assert.assertEquals("M2026-11-03T22:00/2026-11-03T23:00", state.getString("floorHeater.pdlKoupH.times"));
        Assert.assertEquals(570, manager.getTargetMinutes());
        Assert.assertEquals(35, manager.getLaterBonusHaler());
    }

    @Test
    public void invalidRangesAreRejected() {
        try {
            handler.processAction(handler.getItemById("pdlKoupH"), params("ranges", "M2026-11-03T22:00/2026-11-03T22:00"));
            Assert.fail("expected an exception");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Range 1 must end after it starts.", e.getMessage());
        }
    }
}
