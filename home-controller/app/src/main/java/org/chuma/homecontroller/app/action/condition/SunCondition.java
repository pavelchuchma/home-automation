package org.chuma.homecontroller.app.action.condition;

import org.apache.log4j.Logger;
import org.chuma.homecontroller.app.SunCalculator;
import org.chuma.homecontroller.controller.action.condition.ICondition;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class SunCondition implements ICondition {
    static Logger log = Logger.getLogger(SunCondition.class.getName());
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
            log.info("Sun is shining instead of me. Ignoring switch on action!");
            return false;
        }
        return true;
    }
}
