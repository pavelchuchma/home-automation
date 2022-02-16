package org.chuma.homecontroller.controller.action;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.controller.action.condition.ICondition;
import org.chuma.homecontroller.controller.action.condition.SensorDimCounter;
import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class AbstractSensorAction extends AbstractAction {
    public static final int BLINK_DELAY = 600;
    public static final int MAX_BLINK_DURATION = 10_000;
    static Logger log = LoggerFactory.getLogger(AbstractSensorAction.class.getName());
    private final double switchOnValue;
    private final Priority priority;
    private final ICondition condition;
    int timeoutMs;
    boolean canSwitchOn;

    protected AbstractSensorAction(IOnOffActor actor, int timeoutSec, boolean canSwitchOn, double switchOnValue, Priority priority, ICondition condition) {
        super(actor);
        Validate.inclusiveBetween(0,1, switchOnValue);

        this.priority = priority;
        this.condition = condition;
        this.timeoutMs = timeoutSec * 1000;
        this.canSwitchOn = canSwitchOn;
        this.switchOnValue = switchOnValue;
    }

    private boolean canOverwriteState(IOnOffActor act) {
        return act.getActionData() instanceof ActionData
                && ((ActionData) act.getActionData()).priority.ordinal() <= priority.ordinal();
    }

    @Override
    public void perform(int previousDurationMs) {
        if (condition != null) {
            if (condition.isTrue(previousDurationMs)) {
                log.debug("doing action - condition " + condition + " is true");
            } else {
                log.debug("no action - condition " + condition + " is false");
                return;
            }
        }

        // run body in extra thread because it can be blocking
        new Thread(this::performImpl).start();
    }

    private void performImpl() {
        try {
            IOnOffActor act = (IOnOffActor) getActor();
            log.debug("Performing actor: " + act.toString());
            ActionData aData = new ActionData(priority);
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (act) {
                if (act.isOn() && !canOverwriteState(act)) {
                    log.debug("switched on, but by different action type -> do not touch anything");
                    return;
                }
                if (!act.isOn() && !canSwitchOn) {
                    log.debug("already switched off, nothing to do for canSwitchOn == false");
                    return;
                }

                long endTime = System.currentTimeMillis() + timeoutMs;

                if (canSwitchOn) {
                    log.debug("switching on, setting my data");
                    act.switchOn(switchOnValue, aData);
                } else {
                    log.debug("cannot switch on, setting my data only");
                    act.setActionData(aData);
                }

                if (timeoutMs > MAX_BLINK_DURATION) {
                    log.debug("Going to wait for {} ms", timeoutMs - MAX_BLINK_DURATION);
                    act.wait(timeoutMs - MAX_BLINK_DURATION);
                    log.debug("Woken up");
                }
                if (act.getActionData() != aData) {
                    log.debug("action data modified by other thread, leaving action");
                    return;
                }

                while (System.currentTimeMillis() < endTime) {
                    aData.incrementCount();
                    act.callListenersAndSetActionData(aData);
                    long remains = endTime - System.currentTimeMillis();
                    if (remains > 0) {
                        act.wait((remains < BLINK_DELAY) ? remains : BLINK_DELAY);
                    }
                    if (act.getActionData() != aData) {
                        // there was some external modification
                        return;
                    }
                }

                log.debug("nobody modified actor meanwhile -> can switch off");
                act.switchOff(null);
            }
        } catch (Exception e) {
            log.error("perform() method failed", e);
        }
    }

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH
    }

    static class ActionData implements SensorDimCounter {
        private final Priority priority;
        private int count = 0;

        ActionData(Priority priority) {
            this.priority = priority;
        }

        @Override
        public int getCount() {
            return count;
        }

        void incrementCount() {
            count++;
        }
    }
}