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
    int sunriseMinutes = 6 * 60 + 30;
    int sunsetMinutes = 17 * 60 + 30;

    public SwitchOnSensorAction(IOnOffActor actor, int timeout, int switchOnPercent) {
        this(actor, timeout, switchOnPercent, Priority.LOW, NOT_SET, NOT_SET);
    }

    public SwitchOnSensorAction(IOnOffActor actor, int timeout, int switchOnPercent, Priority priority) {
        this(actor, timeout, switchOnPercent, priority, NOT_SET, NOT_SET);
    }

    public SwitchOnSensorAction(IOnOffActor actor, int timeout, int switchOnPercent, int disabledBeforeSunRiseMinutes, int enabledAfterSunsetMinutes) {
        this(actor, timeout, switchOnPercent, Priority.LOW, disabledBeforeSunRiseMinutes, enabledAfterSunsetMinutes);
    }

    public SwitchOnSensorAction(IOnOffActor actor, int timeout, int switchOnPercent, Priority priority, int disabledBeforeSunRiseMinutes, int enabledAfterSunsetMinutes) {
        super(actor, timeout, true, switchOnPercent, priority);
        this.disabledBeforeSunRiseMinutes = disabledBeforeSunRiseMinutes;
        this.enabledAfterSunsetMinutes = enabledAfterSunsetMinutes;
    }

    @Override
    public void perform(int previousDurationMs) {
        if (disabledBeforeSunRiseMinutes != NOT_SET) {
            GregorianCalendar now = new GregorianCalendar();
            int minutesToday = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            if (minutesToday > sunriseMinutes - disabledBeforeSunRiseMinutes && minutesToday < sunsetMinutes + enabledAfterSunsetMinutes) {
                // sun should be shining enough :-)
                log.info("Sun is shining instead of me. Ignoring switch on action!");
                return;
            }
        }
        super.perform(previousDurationMs);
    }
}