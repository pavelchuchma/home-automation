package org.chuma.homecontroller.extensions.external;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import junit.framework.TestCase;
import org.junit.Test;

public class SunCalculatorTest extends TestCase {
    @Test
    public void test() throws Exception {
        SunCalculator c = SunCalculator.getInstance();
        int rise = c.getSunriseMinutes();
        int set = c.getSunsetMinutes();

        GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        int hours = now.get(Calendar.HOUR_OF_DAY);
    }
}