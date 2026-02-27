package org.chuma.homecontroller.extensions.external;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import net.e175.klaus.solarpositioning.SolarPosition;
import org.junit.BeforeClass;
import org.junit.Test;

import org.chuma.homecontroller.base.utils.Options;

public class SunCalculatorTest {
    @BeforeClass
    public static void beforeClass() {
        Options options = new Options("../cfg/app.properties", "default-app.properties");

        SunCalculator.createInstance(options.getDouble("location.latitude"), options.getDouble("location.longitude"), options.getDouble("location.altitude"));
    }

    @Test
    public void testSunrise() {
        SunCalculator c = SunCalculator.getInstance();
        final LocalTime sunrise = c.getSunrise();
        final LocalTime sunset = c.getSunset();
    }

    @Test
    public void testSolarPositioning() {
        ZonedDateTime now = ZonedDateTime.now();
        SolarPosition position = SunCalculator.getInstance().calculateSolarPosition(now);
        System.out.println(position);
    }

    @Test
    public void testSolarPositionInTime() {
        ZonedDateTime t0 = ZonedDateTime.of(2025, 3, 9, 16, 3, 0, 0, ZoneId.of("CET"));
        double a = SunCalculator.getInstance().calculateSolarPosition(t0).azimuth();

        // natoceni baraku: 204.6 st, sklon strechy 33 st
        ZonedDateTime t1 = ZonedDateTime.of(2023, 3, 12, 10, 25, 52, 0, ZoneId.of("CET"));
        double diff1 = 147 - SunCalculator.getInstance().calculateSolarPosition(t1).azimuth();

        ZonedDateTime t2 = ZonedDateTime.of(2023, 3, 12, 12, 56, 07, 0, ZoneId.of("CET"));
        double diff2 = 193 - SunCalculator.getInstance().calculateSolarPosition(t2).azimuth();

        ZonedDateTime t3 = ZonedDateTime.of(2023, 3, 12, 17, 4, 35, 0, ZoneId.of("CET"));
        double diff3 = 254 - SunCalculator.getInstance().calculateSolarPosition(t3).azimuth();
//
        System.out.println("done");
    }
}