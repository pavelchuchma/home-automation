package org.chuma.homecontroller.app.servlet.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import org.chuma.homecontroller.extensions.actor.HvacActor;
import org.chuma.hvaccontroller.IPacketSource;
import org.chuma.hvaccontroller.device.FanSpeed;
import org.chuma.hvaccontroller.device.HvacDevice;
import org.chuma.hvaccontroller.device.OperatingMode;
import org.chuma.hvaccontroller.packet.PacketData;
import org.chuma.hvaccontroller.packet.PacketType;
import org.chuma.hvaccontroller.packet.SetPacketRequest;

public class HvacHandlerTest {
    private static final int UNIT = 0x20;
    private static final int CONTROLLER = 0x85;

    /**
     * Replays the given packets once and records everything sent to the bus.
     */
    private static class FakeBus implements IPacketSource {
        final List<PacketData> sent = new ArrayList<>();
        private final Iterator<PacketData> received;

        FakeBus(List<PacketData> received) {
            this.received = received.iterator();
        }

        @Override
        public synchronized PacketData getPacket() {
            return received.hasNext() ? received.next() : null;
        }

        @Override
        public void startRead() {
        }

        @Override
        public void stopRead() {
        }

        @Override
        public synchronized void sendData(PacketData data) {
            sent.add(data);
        }
    }

    /**
     * State reply of a unit that is on, cooling to 23 °C with fan speed 1.
     */
    private static PacketData coolingState() {
        int[] data = {23 + 55, 24 + 55, 14 + 55, 0x02, 0x80 | 0x02, 0, 0, 21 + 55};
        return new PacketData(UNIT, 0x84, PacketType.CMD_GET_52, data);
    }

    private static HvacHandler handler(HvacDevice device) {
        return new HvacHandler(new HvacActor(device, "hvac", "HVAC"));
    }

    private static HvacDevice startDevice(FakeBus bus, boolean waitForState) throws Exception {
        HvacDevice device = new HvacDevice(bus, CONTROLLER, UNIT, null);
        device.start();
        if (waitForState) {
            long deadline = System.currentTimeMillis() + 5_000;
            while (device.getCurrentMode() == OperatingMode.NONE && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            Assert.assertEquals(OperatingMode.COOL, device.getCurrentMode());
        }
        return device;
    }

    private static SetPacketRequest onlySent(FakeBus bus) {
        Assert.assertEquals(1, bus.sent.size());
        PacketData data = bus.sent.get(0);
        Assert.assertEquals(PacketType.CMD_SET, data.command);
        Assert.assertEquals(UNIT, data.to);
        return new SetPacketRequest(data);
    }

    private static Map<String, String[]> params(String... keyValues) {
        Map<String, String[]> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], new String[]{keyValues[i + 1]});
        }
        return map;
    }

    @Test
    public void missingParamsKeepCurrentValues() throws Exception {
        FakeBus bus = new FakeBus(List.of(coolingState()));
        HvacDevice device = startDevice(bus, true);
        HvacHandler handler = handler(device);

        handler.processAction(handler.getItemById("hvac"), params("id", "hvac", "temp", "21"));

        SetPacketRequest set = onlySent(bus);
        Assert.assertTrue(set.isOn());
        Assert.assertEquals(OperatingMode.COOL, set.getMode());
        Assert.assertEquals(FanSpeed.SPEED_1, set.getFanSpeed());
        Assert.assertEquals(21, set.getTargetTemperature());
        Assert.assertFalse(set.isSleep());
        Assert.assertFalse(set.isQuite());
    }

    @Test
    public void allParamsInOnePacket() throws Exception {
        FakeBus bus = new FakeBus(List.of(coolingState()));
        HvacDevice device = startDevice(bus, true);
        HvacHandler handler = handler(device);

        handler.processAction(handler.getItemById("hvac"), params("id", "hvac", "on", "false", "mode", "HEAT",
                "fan", "SPEED_3", "temp", "25", "sleep", "true", "quiet", "true"));

        SetPacketRequest set = onlySent(bus);
        Assert.assertFalse(set.isOn());
        Assert.assertEquals(OperatingMode.HEAT, set.getMode());
        Assert.assertEquals(FanSpeed.SPEED_3, set.getFanSpeed());
        Assert.assertEquals(25, set.getTargetTemperature());
        Assert.assertTrue(set.isSleep());
        Assert.assertTrue(set.isQuite());
    }

    @Test
    public void switchOffSendsOnlyPower() throws Exception {
        FakeBus bus = new FakeBus(List.of(coolingState()));
        HvacDevice device = startDevice(bus, true);

        Assert.assertTrue(new HvacActor(device, "hvac", "HVAC").switchOff(null));

        SetPacketRequest set = onlySent(bus);
        Assert.assertFalse(set.isOn());
        Assert.assertEquals(OperatingMode.COOL, set.getMode());
        Assert.assertEquals(FanSpeed.SPEED_1, set.getFanSpeed());
        Assert.assertEquals(23, set.getTargetTemperature());
    }

    @Test
    public void temperatureOutOfRangeIsRejected() throws Exception {
        FakeBus bus = new FakeBus(List.of(coolingState()));
        HvacDevice device = startDevice(bus, true);
        HvacHandler handler = handler(device);

        Assert.assertThrows(IllegalArgumentException.class,
                () -> handler.processAction(handler.getItemById("hvac"), params("id", "hvac", "temp", "31")));
        Assert.assertTrue(bus.sent.isEmpty());
    }

    @Test
    public void nothingSentBeforeFirstState() throws Exception {
        FakeBus bus = new FakeBus(List.of());
        HvacDevice device = startDevice(bus, false);
        HvacHandler handler = handler(device);

        Assert.assertThrows(IllegalArgumentException.class,
                () -> handler.processAction(handler.getItemById("hvac"), params("id", "hvac", "on", "true")));
        Assert.assertTrue(bus.sent.isEmpty());
    }
}
