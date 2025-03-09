package org.chuma.homecontroller.extensions.action.condition;

import java.time.LocalTime;
import java.time.ZonedDateTime;

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
        final LocalTime now = ZonedDateTime.now().toLocalTime();
        if (sunCalculator.getSunrise().minusMinutes(disabledBeforeSunRiseMinutes).isBefore(now)
                && sunCalculator.getSunset().plusMinutes(enabledAfterSunsetMinutes).isAfter(now)) {
            log.trace("Sun is shining instead of me. Ignoring switch on action!");
            return false;
        }
        return true;
    }
}
