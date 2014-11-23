package controller.Action;

import controller.actor.IOnOffActor;
import org.apache.log4j.Logger;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class SwitchOnSensorAction extends AbstractSensorAction {
    static Logger log = Logger.getLogger(SwitchOnSensorAction.class.getName());

    public static final int NOT_SET = Integer.MIN_VALUE;
    protected int disabledBeforeSunRiseMinutes;
    protected int enabledAfterSunsetMinutes;
    int sunriseMinutes = 7 * 60 + 7;
    int sunsetMinutes = 16 * 60 + 0;

    public SwitchOnSensorAction(IOnOffActor actor, int timeout) {
        super(actor, timeout, true);
        disabledBeforeSunRiseMinutes = enabledAfterSunsetMinutes = NOT_SET;
    }

    public SwitchOnSensorAction(IOnOffActor actor, int timeout, int disabledBeforeSunRiseMinutes, int enabledAfterSunsetMinutes) {
        super(actor, timeout, true);
    }

    @Override
    public void perform() {
        if (disabledBeforeSunRiseMinutes != NOT_SET) {
            GregorianCalendar now = new GregorianCalendar();
            int minutesToday = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            if (minutesToday > sunriseMinutes - disabledBeforeSunRiseMinutes && minutesToday < sunsetMinutes + enabledAfterSunsetMinutes) {
                // sun should be shining enough :-)
                log.info("Sun is shining instead of me. Ignoring switch on action!");
                return;
            }
        }
        super.perform();
    }
}