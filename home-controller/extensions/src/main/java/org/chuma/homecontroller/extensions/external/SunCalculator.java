package org.chuma.homecontroller.extensions.external;

import java.time.LocalTime;
import java.time.ZonedDateTime;

import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.SPA;
import net.e175.klaus.solarpositioning.SolarPosition;
import net.e175.klaus.solarpositioning.SunriseResult;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.extensions.external.inverter.impl.HttpJsonClient;

public class SunCalculator {
    static Logger log = LoggerFactory.getLogger(SunCalculator.class.getName());
    private static SunCalculator instance;
    private final double latitude;
    private final double longitude;
    private final double altitude;
    private SunriseResult.RegularDay sunriseResult;
    private ZonedDateTime transitSetCalculationTime;

    private SunCalculator(double latitude, double longitude, double altitude) {
        Validate.inclusiveBetween(-90, 90, latitude);
        Validate.inclusiveBetween(-180, 180, longitude);
        Validate.inclusiveBetween(-100, 8848, altitude);
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        transitSetCalculationTime = null;
    }

    public static void createInstance(double latitude, double longitude, double altitude) {
        instance = new SunCalculator(latitude, longitude, altitude);
    }

    public static synchronized SunCalculator getInstance() {
        Validate.notNull(instance, "createInstance() should be called first");
        return instance;
    }

    private SunriseResult.RegularDay getUpdatedSunriseTransitSet() {
        final ZonedDateTime now = ZonedDateTime.now();
        if (transitSetCalculationTime == null || transitSetCalculationTime.plusHours(24).isBefore(now) || transitSetCalculationTime.getZone() != now.getZone()) {
            var result = SPA.calculateSunriseTransitSet(
                    now, latitude, longitude, DeltaT.estimate(now.toLocalDate()), SPA.Horizon.SUNRISE_SUNSET);
            if (result instanceof SunriseResult.RegularDay regularDay) {
                sunriseResult = regularDay;
                transitSetCalculationTime = now;
            } else {
                throw new IllegalStateException("SunriseResult is not a SunriseResult.RegularDay - not supported latitude");
            }
        }
        return sunriseResult;
    }

    SolarPosition calculateSolarPosition(ZonedDateTime time) {
        return SPA.calculateSolarPosition(time, latitude, longitude, altitude,
                DeltaT.estimate(time.toLocalDate()), 1010, 11);
    }

    public LocalTime getSunrise() {
        return getUpdatedSunriseTransitSet().sunrise().toLocalTime();
    }

    public LocalTime getSunset() {
        return getUpdatedSunriseTransitSet().sunset().toLocalTime();
    }
}