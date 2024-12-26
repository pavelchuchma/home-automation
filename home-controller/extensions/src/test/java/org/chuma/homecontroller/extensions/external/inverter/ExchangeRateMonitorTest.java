package org.chuma.homecontroller.extensions.external.inverter;

import junit.framework.TestCase;
import org.junit.Assert;

public class ExchangeRateMonitorTest extends TestCase {
    public void testGetExchangeRate() throws Throwable {
        ExchangeRateMonitor monitor = new ExchangeRateMonitor();
        // first calls should return -1 and run single thread in background
        for (int i = 0; i < 3; i++) {
            long start = System.currentTimeMillis();
            Double v = monitor.getCurrentEurCzkExchangeRate();
            Assert.assertNull(v);
            long end = System.currentTimeMillis();
            Assert.assertTrue(end - start < 100);
        }
        Thread.sleep(1000);
        // calls after some time should return cached value
        for (int i = 0; i < 3; i++) {
            long start = System.currentTimeMillis();
            Double v = monitor.getCurrentEurCzkExchangeRate();
            Assert.assertEquals(25, v, 5);
            long end = System.currentTimeMillis();
            Assert.assertTrue(end - start < 100);
        }
    }
}