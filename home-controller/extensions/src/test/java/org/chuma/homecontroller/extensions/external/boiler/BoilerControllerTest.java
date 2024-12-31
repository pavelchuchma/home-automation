package org.chuma.homecontroller.extensions.external.boiler;

import java.net.UnknownHostException;

import junit.framework.TestCase;

public class BoilerControllerTest extends TestCase {

    public static final String BOILER_ADDRESS = "boiler.local";

    public static class DurationAsserter implements AutoCloseable {
        private final long startTime = System.currentTimeMillis();
        private final int minDuration;
        private final int maxDuration;

        public DurationAsserter(int minDuration, int maxDuration) {
            this.minDuration = minDuration;
            this.maxDuration = maxDuration;
        }

        @Override
        public void close() {
            long duration = System.currentTimeMillis() - startTime;
            assertTrue("too fast: " + duration + " < " + minDuration, duration >= minDuration);
            assertTrue("too slow: " + duration + " > " + maxDuration, duration <= maxDuration);
        }
    }

    public void testGetState() throws UnknownHostException {
        BoilerController bc = new BoilerController(BOILER_ADDRESS);

        State state;
        try (DurationAsserter ignored = new DurationAsserter(0, 1000)) {
            state = bc.getState();
        }
        assertTrue(state.getStatusAge() > 10);

        try (DurationAsserter ignored = new DurationAsserter(4000, 15000)) {
            bc.refreshStatus();
        }

        State state2;
        try (DurationAsserter ignored = new DurationAsserter(0, 1000)) {
            state2 = bc.getState();
        }
        assertTrue(state2.getStatusAge() < 5);
    }

    public void testSetTargetTemp() throws UnknownHostException {
        BoilerController bc = new BoilerController(BOILER_ADDRESS);
        bc.refreshStatus();
        // get current target temp
        State state = bc.getState();
        int origTargetTemp = state.getTargetTemp();
        int newTargetTemp = (origTargetTemp < 57) ? origTargetTemp + 3 : origTargetTemp - 3;

        // modify & check new target temp
        try (DurationAsserter ignored = new DurationAsserter(100, 4000)) {
            bc.setTargetTemp(newTargetTemp);
        }
        bc.refreshStatus();
        State state2 = bc.getState();
        assertEquals(newTargetTemp, state2.getTargetTemp());

        // restore original temp
        bc.setTargetTemp(origTargetTemp);
        State state3 = bc.getState();
        assertEquals(origTargetTemp, state3.getTargetTemp());
    }

    public void testBoilerMonitor() throws InterruptedException {
        BoilerMonitor monitor = new BoilerMonitor(BOILER_ADDRESS, 60_000, 5 * 60_000);
        monitor.start();

        State state;
        try (DurationAsserter ignored = new DurationAsserter(0, 1000)) {
            state = monitor.getState();
        }
        assertNull(state);
        Thread.sleep(15_000);
        try (DurationAsserter ignored = new DurationAsserter(0, 1000)) {
            state = monitor.getState();
        }
        assertNotNull(state);
        assertTrue(state.getStatusAge() < 15);

        Thread.sleep(60_000);
        try (DurationAsserter ignored = new DurationAsserter(0, 1000)) {
            state = monitor.getState();
        }
        assertNotNull(state);
        assertTrue(state.getStatusAge() < 25);

        monitor.stop();
    }
}