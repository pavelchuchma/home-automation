package org.chuma.homecontroller.extensions.external.inverter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;

import junit.framework.TestCase;
import org.junit.Assert;

public class ElectricitySpotPriceMonitorTest extends TestCase {
    public void testGetPrice() throws InterruptedException {
        ElectricitySpotPriceMonitor monitor = new ElectricitySpotPriceMonitor(1500, 500, 21);
        // first calls should return -1 and run single thread in background
        for (int i = 0; i < 3; i++) {
            long start = System.currentTimeMillis();
            ElectricitySpotPriceMonitor.Prices dayPrices = monitor.getDayPrices();
            Assert.assertNull(dayPrices);
            long end = System.currentTimeMillis();
            Assert.assertTrue(end - start < 100);
        }
        Thread.sleep(3000);
        // calls after some time should return the cached value
        for (int i = 0; i < 3; i++) {
            long start = System.currentTimeMillis();
            ElectricitySpotPriceMonitor.Prices dayPrices = monitor.getDayPrices();
            Assert.assertNotNull(dayPrices);
            Assert.assertEquals(48, dayPrices.prices().length);
            long end = System.currentTimeMillis();
            Assert.assertTrue(end - start < 100);
        }
    }

    /**
     * Prints all hour entries to stdout, including time with timezone and the price value.
     */
    private static void printHourPrices(ElectricitySpotPriceMonitor.IntervalPrice[] values) {
        if (values == null) {
            System.out.println("<null>");
            return;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm z");
        GregorianCalendar cal = new GregorianCalendar();
        long now = System.currentTimeMillis();
        for (int i = 0; i < values.length; i++) {
            ElectricitySpotPriceMonitor.IntervalPrice hp = values[i];
            cal.setTimeInMillis(hp.time());
            System.out.println(fmt.format(cal.getTime()) + " (" + hp.time() + ") -> " + hp.price());
            fmt.setTimeZone(cal.getTimeZone());
            if (values[i].time() < now && now < values[i].time() + 3_600_000) {
                cal.setTimeInMillis(now);
                System.out.println("  -- now (" + now + ") " + fmt.format(cal.getTime()));
            }
        }
    }

    public void testTimeShift() {
        ElectricitySpotPriceMonitor monitor = new ElectricitySpotPriceMonitor(1500, 500, 21);

        Calendar firstSummerTimeDate = new GregorianCalendar(2024, Calendar.MARCH, 31);
        ElectricitySpotPriceMonitor.IntervalPrice[] summerValues = monitor.cache.getEntryImpl(firstSummerTimeDate);
        printHourPrices(summerValues);
        Assert.assertEquals(23*4, Objects.requireNonNull(summerValues).length);

        Calendar firstWinterTimeDate = new GregorianCalendar(2024, Calendar.OCTOBER, 27);
        ElectricitySpotPriceMonitor.IntervalPrice[] winterValues = monitor.cache.getEntryImpl(firstWinterTimeDate);
        printHourPrices(winterValues);
        Assert.assertEquals(25*4, Objects.requireNonNull(winterValues).length);

        Calendar today = new GregorianCalendar();
        ElectricitySpotPriceMonitor.IntervalPrice[] todayValues = monitor.cache.getEntryImpl(today);
        printHourPrices(todayValues);
        Assert.assertEquals(24*4, Objects.requireNonNull(todayValues).length);
    }
}