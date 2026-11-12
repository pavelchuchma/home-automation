package org.chuma.homecontroller.extensions.external.inverter;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.chuma.homecontroller.extensions.external.inverter.InverterState.BatteryMode;
import org.chuma.homecontroller.extensions.external.inverter.InverterState.Mode;
import org.chuma.homecontroller.extensions.external.inverter.VppBatteryControl.Owner;
import org.chuma.homecontroller.extensions.external.inverter.impl.SolaxInverterModbusClient;

/**
 * Ownership of the battery while a VPP mode runs. The client records the commands instead of writing them,
 * nothing connects to an inverter.
 */
public class VppBatteryControlTest extends TestCase {
    private static class RecordingClient extends SolaxInverterModbusClient {
        final List<String> commands = new ArrayList<>();
        boolean vppSupported = true;

        RecordingClient() throws Exception {
            super("127.0.0.1");
        }

        @Override
        public synchronized boolean isVppSupported() {
            return vppSupported;
        }

        @Override
        public synchronized void vppPvBattery(int pvLimitW, int batteryW, int durationS) {
            commands.add("pvBattery " + pvLimitW + " " + batteryW + " " + durationS);
        }

        @Override
        public synchronized void vppDisable() {
            commands.add("disable");
        }
    }

    private RecordingClient client;
    private VppBatteryControl control;

    @Override
    protected void setUp() throws Exception {
        client = new RecordingClient();
        control = new VppBatteryControl(client, true);
    }

    /**
     * State with only the values the control may read; any other getter fails the test.
     */
    private static InverterState state(Mode mode, BatteryMode batteryMode, int vppMode) {
        return (InverterState)Proxy.newProxyInstance(InverterState.class.getClassLoader(),
                new Class<?>[]{InverterState.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getMode" -> mode;
                    case "getBatteryMode" -> batteryMode;
                    case "getVppMode" -> vppMode;
                    case "getBatterySoc" -> 39;
                    case "getSelfUseMinimalSoc" -> 100;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static InverterState normal() {
        return state(Mode.Normal, BatteryMode.SelfUse, 0);
    }

    public void testHoldStartsAndRefreshes() {
        assertTrue(control.hold(Owner.FORCE_DISCHARGE, normal(), 30000, 5900));
        assertEquals(Owner.FORCE_DISCHARGE, control.getOwner());
        // the next minute the inverter reports the VPP run mode
        assertTrue(control.hold(Owner.FORCE_DISCHARGE, state(Mode.RemoteControl, BatteryMode.SelfUse, 8), 30000, 4000));

        assertEquals(List.of("pvBattery 30000 5900 180", "pvBattery 30000 4000 180"), client.commands);
    }

    public void testReleaseEndsTheMode() {
        control.hold(Owner.FORCE_DISCHARGE, normal(), 30000, 5900);
        client.commands.clear();

        assertTrue(control.release(Owner.FORCE_DISCHARGE));

        assertEquals(List.of("disable"), client.commands);
        assertEquals(Owner.NONE, control.getOwner());
    }

    public void testReleaseWithoutHoldingSendsNothing() {
        assertFalse(control.release(Owner.FORCE_DISCHARGE));
        assertFalse(control.release(Owner.NONE));

        assertEquals(List.of(), client.commands);
    }

    public void testNoStartFromIdle() {
        assertFalse(control.hold(Owner.FORCE_DISCHARGE, state(Mode.Idle, BatteryMode.SelfUse, 0), 30000, 5900));

        assertEquals(List.of(), client.commands);
        assertEquals(Owner.NONE, control.getOwner());
    }

    public void testNoStartFromManualWorkMode() {
        assertFalse(control.hold(Owner.FORCE_DISCHARGE, state(Mode.Normal, BatteryMode.Manual, 0), 30000, 5900));

        assertEquals(List.of(), client.commands);
    }

    public void testNoStartWithoutVppFirmware() {
        client.vppSupported = false;

        assertFalse(control.hold(Owner.FORCE_DISCHARGE, normal(), 30000, 5900));

        assertEquals(List.of(), client.commands);
    }

    public void testHeldModeIsEndedOnceTheInverterStops() {
        control.hold(Owner.FORCE_DISCHARGE, normal(), 30000, 5900);
        client.commands.clear();

        assertFalse(control.hold(Owner.FORCE_DISCHARGE, state(Mode.Fault, BatteryMode.SelfUse, 0), 30000, 5900));

        assertEquals(List.of("disable"), client.commands);
        assertEquals(Owner.NONE, control.getOwner());
    }

    public void testRefusalLastsWhileTheModeIsWanted() {
        InverterState idle = state(Mode.Idle, BatteryMode.SelfUse, 0);
        assertNull(control.getRefusal(Owner.FORCE_DISCHARGE));

        control.hold(Owner.FORCE_DISCHARGE, idle, 30000, 5900);
        control.hold(Owner.FORCE_DISCHARGE, idle, 30000, 5900);
        assertEquals("the inverter is Idle", control.getRefusal(Owner.FORCE_DISCHARGE));

        // no longer wanted: the next refusal is reported again
        control.release(Owner.FORCE_DISCHARGE);
        assertNull(control.getRefusal(Owner.FORCE_DISCHARGE));
    }

    public void testRefusalEndsWhenTheModeStarts() {
        control.hold(Owner.FORCE_DISCHARGE, state(Mode.Idle, BatteryMode.SelfUse, 0), 30000, 5900);

        assertTrue(control.hold(Owner.FORCE_DISCHARGE, normal(), 30000, 5900));

        assertNull(control.getRefusal(Owner.FORCE_DISCHARGE));
    }

    public void testBatteryRefusalNamesTheOwner() {
        assertNull(control.getBatteryRefusal());

        control.hold(Owner.FORCE_DISCHARGE, state(Mode.Idle, BatteryMode.SelfUse, 0), 30000, 5900);
        assertEquals(new InverterManager.BatteryRefusal("FORCE_DISCHARGE", "the inverter is Idle"),
                control.getBatteryRefusal());

        control.hold(Owner.FORCE_DISCHARGE, normal(), 30000, 5900);
        assertNull(control.getBatteryRefusal());
    }

    public void testNoExportRefusesDischarge() {
        VppBatteryControl noExport = new VppBatteryControl(client, false);

        assertFalse(noExport.hold(Owner.FORCE_DISCHARGE, normal(), 0, 5900));
        assertFalse(noExport.hold(Owner.FORCE_DISCHARGE, normal(), 0, 1));

        assertEquals(List.of(), client.commands);
        assertEquals(Owner.NONE, noExport.getOwner());
        assertEquals("export is not allowed in this deployment", noExport.getRefusal(Owner.FORCE_DISCHARGE));
    }

    public void testNoExportRefusesAnyPv() {
        VppBatteryControl noExport = new VppBatteryControl(client, false);

        // even with the battery charging, PV above the house load would go to the grid
        assertFalse(noExport.hold(Owner.FORCE_DISCHARGE, normal(), 30000, -4800));
        assertFalse(noExport.hold(Owner.FORCE_DISCHARGE, normal(), 1, 0));

        assertEquals(List.of(), client.commands);
    }

    public void testNoExportAllowsChargingWithPvOff() {
        VppBatteryControl noExport = new VppBatteryControl(client, false);

        assertTrue(noExport.hold(Owner.FORCE_DISCHARGE, normal(), 0, -4800));
        assertTrue(noExport.hold(Owner.FORCE_DISCHARGE, normal(), 0, 0));

        assertEquals(List.of("pvBattery 0 -4800 180", "pvBattery 0 0 180"), client.commands);
    }

    public void testLeftoverModeIsEndedOnce() {
        control.clearLeftoverOnce(state(Mode.RemoteControl, BatteryMode.SelfUse, 8));
        control.clearLeftoverOnce(state(Mode.RemoteControl, BatteryMode.SelfUse, 8));

        assertEquals(List.of("disable"), client.commands);
    }

    public void testNoLeftoverSendsNothing() {
        control.clearLeftoverOnce(normal());

        assertEquals(List.of(), client.commands);
    }

    public void testOwnModeIsNotTakenForLeftover() {
        control.hold(Owner.FORCE_DISCHARGE, normal(), 30000, 5900);
        client.commands.clear();

        control.clearLeftoverOnce(state(Mode.RemoteControl, BatteryMode.SelfUse, 8));

        assertEquals(List.of(), client.commands);
    }

    public void testRefusalReasons() {
        assertNull(VppBatteryControl.getRefusal(true, Mode.Normal, BatteryMode.SelfUse));
        assertNull(VppBatteryControl.getRefusal(true, Mode.RemoteControl, BatteryMode.FeedInPriority));
        assertNotNull(VppBatteryControl.getRefusal(false, Mode.Normal, BatteryMode.SelfUse));
        assertNotNull(VppBatteryControl.getRefusal(true, Mode.Normal, BatteryMode.Manual));
        assertNotNull(VppBatteryControl.getRefusal(true, Mode.Idle, BatteryMode.SelfUse));
        assertNotNull(VppBatteryControl.getRefusal(true, Mode.Waiting, BatteryMode.SelfUse));
        assertNotNull(VppBatteryControl.getRefusal(true, Mode.Unknown, BatteryMode.SelfUse));
    }
}
