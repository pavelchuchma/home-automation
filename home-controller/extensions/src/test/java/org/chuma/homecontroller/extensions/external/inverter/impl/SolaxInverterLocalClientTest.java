package org.chuma.homecontroller.extensions.external.inverter.impl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import junit.framework.TestCase;
import org.junit.Assert;

public class SolaxInverterLocalClientTest extends TestCase {

    private String getPassword() throws IOException {
        String s = System.getProperty("user.dir");
        try (BufferedReader br = new BufferedReader(new FileReader("../cfg/app.properties"))) {
            String line;
            while ((line = br.readLine()) != null) {
                final String[] vals = line.split("=");
                if (vals.length == 2 && "inverter.local.password".equals(vals[0])) {
                    return vals[1];
                }
            }
        }
        throw new IllegalStateException("Cannot get file from config file");
    }

    public void testGetState() throws Exception {
        SolaxInverterLocalClient s = new SolaxInverterLocalClient("http://192.168.68.159", getPassword());

        final SolaxInverterState state = s.getState();

        int pv = state.pv1Power;
        int grid = state.grid1Power + state.grid2Power + state.grid3Power;
        int battery = state.batteryPower;
        int feedIn = state.feedInPower;

        System.out.println("Mode: " + state.mode);
        System.out.println("PV: " + pv + " W");
        System.out.println("AC Power: " + grid + " W");
        System.out.println("FeedIn: " + feedIn + " W");
        System.out.println("Battery: " + battery + " W");
        System.out.println("Battery SOC: " + state.batterySoc + "%");
        System.out.println();
        System.out.println("Yield today: " + state.yieldToday + " W");
        System.out.println("Consumed today: " + state.consumedEnergyToday + " W");
        System.out.println();
        int load = grid - feedIn;
        int diff = pv - (battery + grid);
        System.out.println("Load: " + load + " W");
        System.out.println("Diff: " + diff + " W");
    }

    public void testMonitor() throws Exception {
        SolaxInverterMonitor monitor = new SolaxInverterMonitor(
                "http://192.168.68.159", getPassword(), 3_000, 8_000);

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
        final SolaxInverterState s1 = monitor.state;
        Assert.assertNotNull("should be refreshed after 2s", s1);
        final SolaxInverterState s2 = monitor.getState();
        Assert.assertEquals("the same instance expected", s1, s2);
        Thread.sleep(3_000);
        final SolaxInverterState s3 = monitor.state;
        Assert.assertNotEquals("a fresh instance expected", s2, s3);
        Assert.assertEquals("the same instance expected", s3, monitor.getState());
        Thread.sleep(10_000);

        // refresh should be stopped after 10s
        Assert.assertNull("should be null after unused for long time", monitor.state);

        // call getter to restart refresh thread
        Assert.assertNull("null expected", monitor.getState());
        Thread.sleep(5_000);
        final SolaxInverterState s4 = monitor.getState();
        Assert.assertNotNull("a fresh instance expected", s4);

        System.out.println("Stopping");
        monitor.stop();
        Thread.sleep(4_000);
        final SolaxInverterState s6 = monitor.state;
        Thread.sleep(4_000);
        Assert.assertEquals("the same instance expected, because it should be stopped", s6, monitor.state);
        Assert.assertThrows(IllegalStateException.class, monitor::getState);
    }
}