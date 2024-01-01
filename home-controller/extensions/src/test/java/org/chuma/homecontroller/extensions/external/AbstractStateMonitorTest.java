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
    public static <S> AbstractStateMonitor.State<S> getRawState(AbstractStateMonitor<S> m) {
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

    public static <S> void processMonitorStateTest(AbstractStateMonitor<S> monitor, int expectedRefreshDuration) {
        Assert.assertThrows(IllegalStateException.class, monitor::getState);
        log.debug("Starting");
        monitor.start();
        log.debug("Started");
        sleep(1_000);
        log.debug("Getting state");
        Assert.assertNull("should be null at the beginning", AbstractStateMonitorTest.getRawState(monitor));
        Assert.assertNull(monitor.getState());
        Assert.assertNull("should be still null immediately after the first get", AbstractStateMonitorTest.getRawState(monitor));
        sleep(expectedRefreshDuration);

        // active wait for initial get
        final int sleepStep = 250;
        for (int t = 0; t < 3 * expectedRefreshDuration; t += sleepStep) {
            if (AbstractStateMonitorTest.getRawState(monitor) != null) {
                break;
            }
            sleep(sleepStep);
        }

        final S s1 = monitor.getState();
        if (s1 == null) {
            // wait a while for exception after timeout
            sleep(10_000);
        }
        Assert.assertNotNull("should be refreshed", s1);
        final S s2 = monitor.getState();
        Assert.assertEquals("the same instance expected", s1, s2);

        // wait for next refresh
        sleep(monitor.refreshInternalMs + 100 + expectedRefreshDuration);
        final S s3 = monitor.getState();
        Assert.assertNotEquals("a fresh instance expected", s2, s3);
        Assert.assertEquals("the same instance expected", s3, monitor.getState());
        Assert.assertEquals("the same instance expected", s3, monitor.getStateSync(false));
        Assert.assertNotEquals("a fresh instance expected", s3, monitor.getStateSync(true));

        // wait for unused timeout, should be null then
        sleep(monitor.maxUnusedRunTimeMs + 100);
        Assert.assertNull("should be null after unused for long time", AbstractStateMonitorTest.getRawState(monitor));

        final S s3s1 = monitor.getStateSync(false);
        Assert.assertNotNull("should be fresh got synchronously", s3s1);
        final S s3s2 = monitor.getStateSync(false);
        Assert.assertEquals("the same instance expected", s3s1, s3s2);
        Assert.assertEquals("the same instance expected as it was returned from the sync call", s3s1, monitor.getState());

        // wait for unused timeout, should be null then
        sleep(monitor.maxUnusedRunTimeMs + 100);
        Assert.assertNull("should be null after unused for long time", AbstractStateMonitorTest.getRawState(monitor));

        // call getter to restart refresh thread
        Assert.assertNull("null expected", monitor.getState());
        sleep(expectedRefreshDuration);
        final S s4 = monitor.getState();
        Assert.assertNotNull("a fresh instance expected", s4);

        log.debug("Stopping");
        monitor.stop();
        Assert.assertNull("should be null after stop for long time", AbstractStateMonitorTest.getRawState(monitor));
        Assert.assertThrows(IllegalStateException.class, monitor::getState);
    }

    public static <S> void processGetBeforeStartTest(AbstractStateMonitor<S> monitor, int expectedRefreshDuration) {
        final S s1 = monitor.getStateSync(false);
        Assert.assertNotNull(s1);
        Assert.assertNull("should be null before start", AbstractStateMonitorTest.getRawState(monitor));
        final S s1b = monitor.getStateSync(false);
        Assert.assertNotNull(s1b);
        Assert.assertNotEquals("a fresh instance expected", s1, s1b);
        Assert.assertNull("should be null before start", AbstractStateMonitorTest.getRawState(monitor));

        // not started, should fail
        Assert.assertThrows(IllegalStateException.class, monitor::getState);

        monitor.start();
        Assert.assertNull("null expected", monitor.getState());
        sleep(expectedRefreshDuration);
        final S s2 = monitor.getState();
        Assert.assertNotNull(s2);
        Assert.assertNotEquals("a fresh instance expected", s1, s2);
        Assert.assertNotEquals("a fresh instance expected", s1b, s2);
        final S s3 = monitor.getStateSync(false);
        Assert.assertEquals("the same instance expected", s2, s3);

        monitor.stop();
        Assert.assertNull("should be null after stop", AbstractStateMonitorTest.getRawState(monitor));
        final S s4 = monitor.getStateSync(false);
        Assert.assertNotNull(s4);
        Assert.assertNull("should be null after stop", AbstractStateMonitorTest.getRawState(monitor));
        Assert.assertNotEquals("a fresh instance expected", s3, s4);
    }
}