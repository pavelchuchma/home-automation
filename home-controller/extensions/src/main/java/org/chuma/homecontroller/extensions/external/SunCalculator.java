package org.chuma.homecontroller.extensions.external;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SunCalculator {
    static Logger log = LoggerFactory.getLogger(SunCalculator.class.getName());
    private static SunCalculator instance;
    private SunriseSunsetCalculator calculator;
    private TimeZone timeZone;
    private int sunriseMinutes;
    private int sunsetMinutes;
    private Date lastComputeDate;

    private SunCalculator() {
        timeZone = TimeZone.getTimeZone("UTC");
        lastComputeDate = new Date(0);
        calculator = getSunriseSunsetCalculator();
        compute();
    }

    public static synchronized SunCalculator getInstance() {
        if (instance == null) {
            instance = new SunCalculator();
        }
        return instance;
    }

    private SunriseSunsetCalculator getSunriseSunsetCalculator() {
        Location location = new Location(49.0781636, 18.0231517);
        return new SunriseSunsetCalculator(location, timeZone);
    }

    private void compute() {
        Calendar now = Calendar.getInstance(timeZone);
        if (now.getTime().getTime() > lastComputeDate.getTime() + 24 * 60 * 60 * 1000) {
            Calendar sunrise = calculator.getOfficialSunriseCalendarForDate(now);
            sunriseMinutes = sunrise.get(Calendar.HOUR_OF_DAY) * 60 + sunrise.get(Calendar.MINUTE);

            Calendar sunset = calculator.getOfficialSunsetCalendarForDate(now);
            sunsetMinutes = sunset.get(Calendar.HOUR_OF_DAY) * 60 + sunset.get(Calendar.MINUTE);
            lastComputeDate = now.getTime();
            log.info("New sunrise & sunset computed. Sunrise: " + sunrise.getTime() + " Sunset: " + sunset.getTime());
        }
    }

    public int getSunriseMinutes() {
        compute();
        return sunriseMinutes;
    }

    public int getSunsetMinutes() {
        compute();
        return sunsetMinutes;
    }
}