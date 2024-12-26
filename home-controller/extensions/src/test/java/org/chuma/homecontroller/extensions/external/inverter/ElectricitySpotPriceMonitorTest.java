package org.chuma.homecontroller.extensions.external.inverter;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;

import junit.framework.TestCase;
import org.junit.Assert;

public class ElectricitySpotPriceMonitorTest extends TestCase {
    public void testGetPrice() throws InterruptedException {
        ElectricitySpotPriceMonitor monitor = new ElectricitySpotPriceMonitor();
        // first calls should return -1 and run single thread in background
        for (int i = 0; i < 3; i++) {
            long start = System.currentTimeMillis();
            ElectricitySpotPriceMonitor.Prices dayPrices = monitor.getDayPrices();
            Assert.assertNull(dayPrices);
            long end = System.currentTimeMillis();
            Assert.assertTrue(end - start < 100);
        }
        Thread.sleep(1000);
        // calls after some time should return cached value
        for (int i = 0; i < 3; i++) {
            long start = System.currentTimeMillis();
            ElectricitySpotPriceMonitor.Prices dayPrices = monitor.getDayPrices();
            Assert.assertNotNull(dayPrices);
            Assert.assertEquals(48, dayPrices.prices.length);
            long end = System.currentTimeMillis();
            Assert.assertTrue(end - start < 100);
        }
    }

    public void testTimeShift() {
        Calendar firstSummerTimeDate = new GregorianCalendar(2024, Calendar.MARCH, 31);
        Calendar firstWinterTimeDate = new GregorianCalendar(2024, Calendar.OCTOBER, 27);
        ElectricitySpotPriceMonitor monitor = new ElectricitySpotPriceMonitor();
        double[] summerValues = monitor.cache.getEntryImpl(firstSummerTimeDate);
        Assert.assertEquals(24, Objects.requireNonNull(summerValues).length);
        double[] winterValues = monitor.cache.getEntryImpl(firstWinterTimeDate);
        Assert.assertEquals(24, Objects.requireNonNull(winterValues).length);
    }
}