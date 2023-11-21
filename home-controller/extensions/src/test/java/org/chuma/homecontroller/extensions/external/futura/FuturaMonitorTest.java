package org.chuma.homecontroller.extensions.external.futura;

import org.junit.Assert;

import org.chuma.homecontroller.extensions.external.AbstractStateMonitorTest;

public class FuturaMonitorTest extends AbstractFuturaTestBase {
    public void testMonitor() throws Exception {
        FuturaMonitor monitor = new FuturaMonitor(futuraIpAddress, 3_000, 8_000);

        Assert.assertThrows(IllegalStateException.class, monitor::getState);
        System.out.println("Starting");
        monitor.start();
        System.out.println("Started");
        Thread.sleep(3_000);
        System.out.println("Getting state");
        Assert.assertNull("should be null at the beginning", AbstractStateMonitorTest.getRawState(monitor));
        Assert.assertNull(monitor.getState());
        Assert.assertNull("should be still null immediately after the first get", AbstractStateMonitorTest.getRawState(monitor));
        Thread.sleep(2_000);
        final State s1 = monitor.getState();
        Assert.assertNotNull("should be refreshed after 2s", s1);
        final State s2 = monitor.getState();
        Assert.assertEquals("the same instance expected", s1, s2);
        Thread.sleep(3_000);
        final State s3 = monitor.getState();
        Assert.assertNotEquals("a fresh instance expected", s2, s3);
        Assert.assertEquals("the same instance expected", s3, monitor.getState());
        Assert.assertEquals("the same instance expected", s3, monitor.getStateSync());
        Thread.sleep(11_000);

        final State s3s1 = monitor.getStateSync();
        Assert.assertNotNull("should be fresh got synchronously", s3s1);
        final State s3s2 = monitor.getStateSync();
        Assert.assertNotNull("should be fresh got synchronously", s3s2);
        Assert.assertNotEquals("a fresh instance expected", s3s1, s3s2);

        // refresh should be stopped after 10s
        Assert.assertNull("should be null after unused for long time", AbstractStateMonitorTest.getRawState(monitor));

        // call getter to restart refresh thread
        Assert.assertNull("null expected", monitor.getState());
        Thread.sleep(5_000);
        final State s4 = monitor.getState();
        Assert.assertNotNull("a fresh instance expected", s4);

        System.out.println("Stopping");
        monitor.stop();
        Thread.sleep(4_000);
        final State s6 = AbstractStateMonitorTest.getRawState(monitor);
        Thread.sleep(4_000);
        Assert.assertEquals("the same instance expected, because it should be stopped", s6, AbstractStateMonitorTest.getRawState(monitor));
        Assert.assertThrows(IllegalStateException.class, monitor::getState);
    }
}