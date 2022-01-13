package org.chuma.homecontroller.extensions.action.condition;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.controller.action.condition.ICondition;
import org.chuma.homecontroller.extensions.external.SunCalculator;

/**
 * Is true if the sun is sleeping (with specified tolerance)
 */
public class SunCondition implements ICondition {
    static Logger log = LoggerFactory.getLogger(SunCondition.class.getName());
    protected int disabledBeforeSunRiseMinutes;
    protected int enabledAfterSunsetMinutes;
    SunCalculator sunCalculator = SunCalculator.getInstance();

    public SunCondition(int disabledBeforeSunRiseMinutes, int enabledAfterSunsetMinutes) {
        this.disabledBeforeSunRiseMinutes = disabledBeforeSunRiseMinutes;
        this.enabledAfterSunsetMinutes = enabledAfterSunsetMinutes;
    }

    @Override
    public boolean isTrue(int previousDurationMs) {
        GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        int minutesToday = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        if (minutesToday > sunCalculator.getSunriseMinutes() - disabledBeforeSunRiseMinutes && minutesToday < sunCalculator.getSunsetMinutes() + enabledAfterSunsetMinutes) {
            // sun should be shining enough :-)
            log.trace("Sun is shining instead of me. Ignoring switch on action!");
            return false;
        }
        return true;
    }
}
