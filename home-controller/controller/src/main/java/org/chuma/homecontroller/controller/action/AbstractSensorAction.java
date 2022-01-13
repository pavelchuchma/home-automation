package org.chuma.homecontroller.controller.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.controller.action.condition.ICondition;
import org.chuma.homecontroller.controller.action.condition.SensorDimCounter;
import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class AbstractSensorAction extends AbstractAction {
    public static final int BLINK_DELAY = 600;
    public static final int MAX_BLINK_DURATION = 10_000;
    static Logger log = LoggerFactory.getLogger(AbstractSensorAction.class.getName());
    private final int switchOnPercent;
    private final Priority priority;
    private final ICondition condition;
    int timeout;
    boolean canSwitchOn;

    protected AbstractSensorAction(IOnOffActor actor, int timeout, boolean canSwitchOn, int switchOnPercent, Priority priority, ICondition condition) {
        super(actor);
        this.priority = priority;
        this.condition = condition;
        this.timeout = timeout * 1000;
        this.canSwitchOn = canSwitchOn;
        this.switchOnPercent = switchOnPercent;
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

                long endTime = System.currentTimeMillis() + timeout;

                if (canSwitchOn) {
                    log.debug("switching on, setting my data");
                    act.switchOn(switchOnPercent, aData);
                } else {
                    log.debug("cannot switch on, setting my data only");
                    act.setActionData(aData);
                }

                if (timeout > MAX_BLINK_DURATION) {
                    log.debug(String.format("Going to wait for %d ms", timeout - MAX_BLINK_DURATION));
                    act.wait(timeout - MAX_BLINK_DURATION);
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