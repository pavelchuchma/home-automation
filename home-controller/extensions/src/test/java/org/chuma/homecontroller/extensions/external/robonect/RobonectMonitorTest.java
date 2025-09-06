package org.chuma.homecontroller.extensions.external.robonect;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.chuma.homecontroller.base.utils.Utils.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.chuma.homecontroller.extensions.external.robonect.client.RobonectClient;
import org.chuma.homecontroller.extensions.external.robonect.client.RobonectClientTest;
import org.chuma.homecontroller.extensions.external.robonect.client.RobonectEndpoint;

public class RobonectMonitorTest {
    private static RobonectEndpoint robonectEndpoint;

    @BeforeClass
    public static void beforeClass() {
        robonectEndpoint = RobonectClientTest.getRobonectEndpoint();
    }

    @Test
    public void testMonitor() {
        RobonectClient client = new RobonectClient(robonectEndpoint);
        RobonectMonitor monitor = new RobonectMonitor(client, 5_000, 100_000);
        monitor.start();
        {
            sleep(1_000);
            State state1 = monitor.getState();
            assertNull(state1);
        }
        State state2;
        {
            sleep(5_000);
            state2 = monitor.getState();
            assertNotNull(state2);
        }
        State state3 = monitor.getStateSync(true);
        State state4 = monitor.getStateSync(false);
        assertSame(state3, state4);
        sleep(100_000);
        monitor.stop();
    }

    @Test
    public void testGpsHistory() {
        RobonectClient client = new RobonectClient(robonectEndpoint);
        RobonectMonitor monitor = new RobonectMonitor(client, 5_000, 100_000);
        for (int i = 0; i < 100; i++) {
            monitor.gpsHistory.addLast(new RobonectMonitor.GpsHistoryEntry(i * 100, i * 200, i + 100));
        }
        RobonectMonitor.GpsHistoryEntry[] history80 = monitor.getGpsHistory(80 + 100);
        assertEquals(19, history80.length);
        assertEquals(181, history80[0].timestamp());
        assertEquals(199, history80[18].timestamp());

        RobonectMonitor.GpsHistoryEntry[] history0 = monitor.getGpsHistory(0);
        assertEquals(100, history0.length);
        assertEquals(100, history0[0].timestamp());
        assertEquals(199, history0[99].timestamp());

        RobonectMonitor.GpsHistoryEntry[] history500 = monitor.getGpsHistory(500);
        assertEquals(0, history500.length);
    }

    @Test
    public void testRelativeGpsHistory() {
        RobonectClient client = new RobonectClient(robonectEndpoint);
        RobonectMonitor monitor = new RobonectMonitor(client, 5_000, 100_000);
        for (int i = 0; i < 5; i++) {
            monitor.gpsHistory.addLast(new RobonectMonitor.GpsHistoryEntry(i * 100, i * 200, monitor.getTimestamp()));
            sleep(1010);
        }
        long now = monitor.getTimestamp();
        RobonectMonitor.GpsHistoryEntry[] historyM4 = monitor.getGpsHistory(-4);
        assertEquals(3, historyM4.length);
        assertEquals(now - 3, historyM4[0].timestamp());
        assertEquals(now - 1, historyM4[2].timestamp());
    }
}