package org.chuma.homecontroller.extensions.external;

import junit.framework.TestCase;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractStateMonitorTest extends TestCase {
    static Logger log = LoggerFactory.getLogger(AbstractStateMonitorTest.class.getName());

    /**
     * Private field getter for tests
     */
    static <S> AbstractStateMonitor.Snapshot<S> getRawState(AbstractStateMonitor<S> m) {
        return m.state;
    }

    private static void sleep(long millis) {
        log.debug("Test is going to sleep for {} ms", millis);
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.debug("Test woke up after {} ms", millis);
    }

    private static <S> void waitForState(AbstractStateMonitor<S> monitor, long maxWaitMs) {
        final int sleepStep = 50;
        for (long t = 0; t < maxWaitMs && getRawState(monitor) == null; t += sleepStep) {
            sleep(sleepStep);
        }
    }

    /**
     * Shared scenario for monitors of real devices: the first refresh runs right after start, the fast rate
     * applies while getState() is being called, and the state is kept and refreshed at the idle rate afterwards.
     */
    public static <S> void processMonitorStateTest(AbstractStateMonitor<S> monitor, int expectedRefreshDuration) {
        Assert.assertThrows(IllegalStateException.class, monitor::getState);
        Assert.assertNull("peek before start", monitor.peekState());
        log.debug("Starting");
        monitor.start();
        log.debug("Started");

        waitForState(monitor, 3L * expectedRefreshDuration + 1_000);
        final S s1 = monitor.getState();
        Assert.assertNotNull("should be refreshed", s1);
        final S s2 = monitor.getState();
        Assert.assertEquals("the same instance expected", s1, s2);

        // wait for next refresh at the fast rate
        sleep(monitor.refreshIntervalMs + 100 + expectedRefreshDuration);
        final S s3 = monitor.getState();
        Assert.assertNotEquals("a fresh instance expected", s2, s3);
        Assert.assertEquals("the same instance expected", s3, monitor.getState());
        Assert.assertEquals("the same instance expected", s3, monitor.getStateSync(false));
        final S s3sync = monitor.getStateSync(true);
        Assert.assertNotEquals("a fresh instance expected", s3, s3sync);

        // nobody watching: the state is kept and refreshed at the idle rate
        sleep(monitor.fastRateDurationMs + monitor.idleRefreshIntervalMs + expectedRefreshDuration + 100);
        AbstractStateMonitor.Snapshot<S> idle = monitor.peekState();
        Assert.assertNotNull("state must be kept while idle", idle);
        Assert.assertNotEquals("idle refresh expected", s3sync, idle.value);
        Assert.assertEquals("getState returns the peeked state", idle.value, monitor.getState());

        log.debug("Stopping");
        monitor.stop();
        Assert.assertNull("should be null after stop", getRawState(monitor));
        Assert.assertNull("peek after stop", monitor.peekState());
        Assert.assertThrows(IllegalStateException.class, monitor::getState);
    }

    public static <S> void processGetBeforeStartTest(AbstractStateMonitor<S> monitor, int expectedRefreshDuration) {
        final S s1 = monitor.getStateSync(false);
        Assert.assertNotNull(s1);
        Assert.assertNull("should be null before start", getRawState(monitor));
        final S s1b = monitor.getStateSync(false);
        Assert.assertNotNull(s1b);
        Assert.assertNotEquals("a fresh instance expected", s1, s1b);
        Assert.assertNull("should be null before start", getRawState(monitor));

        // not started, should fail
        Assert.assertThrows(IllegalStateException.class, monitor::getState);

        monitor.start();
        waitForState(monitor, 3L * expectedRefreshDuration + 1_000);
        final S s2 = monitor.getState();
        Assert.assertNotNull(s2);
        Assert.assertNotEquals("a fresh instance expected", s1, s2);
        Assert.assertNotEquals("a fresh instance expected", s1b, s2);
        final S s3 = monitor.getStateSync(false);
        Assert.assertEquals("the same instance expected", s2, s3);

        monitor.stop();
        Assert.assertNull("should be null after stop", getRawState(monitor));
        final S s4 = monitor.getStateSync(false);
        Assert.assertNotNull(s4);
        Assert.assertNull("should be null after stop", getRawState(monitor));
        Assert.assertNotEquals("a fresh instance expected", s3, s4);
    }

    private static class TestState {
        volatile int refreshCount = 0;
        volatile boolean firstRefresh = false;
    }

    /**
     * Fast rate 200 ms for 1 s after the last use, idle rate 600 ms.
     */
    private static class TestMonitor extends AbstractStateMonitor<TestState> {
        static final int FAST_MS = 200;
        static final int FAST_DURATION_MS = 1_000;
        static final int IDLE_MS = 600;
        final TestState state = new TestState();

        public TestMonitor() {
            super("testMonitor", FAST_MS, FAST_DURATION_MS, IDLE_MS);
        }

        @Override
        protected TestState getStateImpl(boolean firstRefresh) {
            state.refreshCount++;
            state.firstRefresh = firstRefresh;
            return state;
        }
    }

    public void testFirstRefreshRunsImmediately() {
        TestMonitor monitor = new TestMonitor();
        monitor.start();
        try {
            sleep(100);
            assertEquals(1, monitor.state.refreshCount);
            assertTrue(monitor.state.firstRefresh);
            Assert.assertNotNull(monitor.peekState());
            Assert.assertNotNull(monitor.getState());
            Assert.assertNotNull(monitor.getStateSync(false));
            assertEquals(1, monitor.state.refreshCount);
            Assert.assertNotNull(monitor.getStateSync(true));
            assertEquals(2, monitor.state.refreshCount);
            assertFalse(monitor.state.firstRefresh);
        } finally {
            monitor.stop();
        }
    }

    public void testIdleRateWithoutUse() {
        TestMonitor monitor = new TestMonitor();
        monitor.start();
        try {
            // never used: idle rate from the start, 600 ms per refresh
            sleep(100);
            Assert.assertNotNull(monitor.peekState());
            int before = monitor.state.refreshCount;
            sleep(2 * TestMonitor.IDLE_MS + 100);
            int delta = monitor.state.refreshCount - before;
            assertTrue("expected ~2 idle refreshes, got " + delta, delta >= 1 && delta <= 3);
        } finally {
            monitor.stop();
        }
    }

    public void testGetStateSwitchesToFastRateAndBack() {
        TestMonitor monitor = new TestMonitor();
        monitor.start();
        try {
            sleep(100);
            Assert.assertNotNull(monitor.getState());
            int before = monitor.state.refreshCount;
            sleep(TestMonitor.FAST_DURATION_MS);
            int fastDelta = monitor.state.refreshCount - before;
            assertTrue("expected ~5 fast refreshes, got " + fastDelta, fastDelta >= 3 && fastDelta <= 6);

            // fast rate expired, back to idle
            sleep(TestMonitor.IDLE_MS);
            before = monitor.state.refreshCount;
            sleep(2 * TestMonitor.IDLE_MS + 100);
            int idleDelta = monitor.state.refreshCount - before;
            assertTrue("expected ~2 idle refreshes, got " + idleDelta, idleDelta >= 1 && idleDelta <= 3);
        } finally {
            monitor.stop();
        }
    }

    public void testPeekDoesNotSwitchToFastRate() {
        TestMonitor monitor = new TestMonitor();
        monitor.start();
        try {
            sleep(100);
            int before = monitor.state.refreshCount;
            for (int i = 0; i < 5; i++) {
                Assert.assertNotNull(monitor.peekState());
                sleep(TestMonitor.FAST_MS);
            }
            // 1 s at the idle rate is at most two refreshes; the fast rate would give five
            int delta = monitor.state.refreshCount - before;
            assertTrue("peek must not speed up refreshing, got " + delta, delta <= 2);
        } finally {
            monitor.stop();
        }
    }

    public void testPeekReturnsNullWhenRefreshFailed() {
        AbstractStateMonitor<Object> failing = new AbstractStateMonitor<>("failing", 200, 1_000, 600) {
            @Override
            protected Object getStateImpl(boolean firstRefresh) {
                return null;
            }
        };
        failing.start();
        try {
            sleep(100);
            Assert.assertNotNull("refresh ran", getRawState(failing));
            Assert.assertNull("failed refresh is not a state", failing.peekState());
            Assert.assertNull(failing.getState());
        } finally {
            failing.stop();
        }
    }
}
