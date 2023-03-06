package org.chuma.homecontroller.extensions.external.inverter.impl;

import org.junit.Assert;
import org.junit.Test;

import org.chuma.homecontroller.extensions.external.inverter.InverterState;

public class SolaxInverterLocalClientTest extends SolaxInverterTestBase {
    @Test
    public void testGetState() {
        SolaxInverterLocalClient s = new SolaxInverterLocalClient(localUrl, localPassword);

        final InverterState state = s.getState();

        int pv = state.getPv1Power();
        int grid = state.getGrid1Power() + state.getGrid2Power() + state.getGrid3Power();
        int battery = state.getBatteryPower();
        int feedIn = state.getFeedInPower();

        System.out.println("SN: " + state.getInverterSerialNumber());
        System.out.println("Mode: " + state.getMode());
        System.out.println("Battery Mode: " + state.getBatteryMode());
        System.out.println("PV: " + pv + " W");
        System.out.println("AC Power: " + grid + " W");
        System.out.println("FeedIn: " + feedIn + " W");
        System.out.println("Battery: " + battery + " W");
        System.out.println("Battery SOC: " + state.getBatterySoc() + "%");
        System.out.println();
        System.out.println("Yield today: " + state.getYieldToday() + " W");
        System.out.println("Consumed today: " + state.getConsumedEnergyToday() + " W");
        System.out.println();
        int load = grid - feedIn;
        int diff = pv - (battery + grid);
        System.out.println("Load: " + load + " W");
        System.out.println("Diff: " + diff + " W");
    }

    @Test
    public void testMonitor() throws Exception {
        SolaxInverterMonitor monitor = new SolaxInverterMonitor(
                localUrl, localPassword, 3_000, 8_000);

        Assert.assertThrows(IllegalStateException.class, monitor::getState);
        System.out.println("Starting");
        monitor.start();
        System.out.println("Started");
        Thread.sleep(3_000);
        System.out.println("Getting state");
        Assert.assertNull("should be null at the beginning", monitor.state);
        Assert.assertNull(monitor.getState());
        Assert.assertNull("should be still null immediately after the first get", monitor.state);
        Thread.sleep(2_000);
        final InverterState s1 = monitor.getState();
        Assert.assertNotNull("should be refreshed after 2s", s1);
        final InverterState s2 = monitor.getState();
        Assert.assertEquals("the same instance expected", s1, s2);
        Thread.sleep(3_000);
        final InverterState s3 = monitor.getState();
        Assert.assertNotEquals("a fresh instance expected", s2, s3);
        Assert.assertEquals("the same instance expected", s3, monitor.getState());
        Thread.sleep(10_000);

        // refresh should be stopped after 10s
        Assert.assertNull("should be null after unused for long time", monitor.state);

        // call getter to restart refresh thread
        Assert.assertNull("null expected", monitor.getState());
        Thread.sleep(5_000);
        final InverterState s4 = monitor.getState();
        Assert.assertNotNull("a fresh instance expected", s4);

        System.out.println("Stopping");
        monitor.stop();
        Thread.sleep(4_000);
        final InverterState s6 = monitor.state;
        Thread.sleep(4_000);
        Assert.assertEquals("the same instance expected, because it should be stopped", s6, monitor.state);
        Assert.assertThrows(IllegalStateException.class, monitor::getState);
    }
}