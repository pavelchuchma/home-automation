package org.chuma.homecontroller.controller.action;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.controller.action.condition.ICondition;
import org.chuma.homecontroller.controller.action.condition.SensorDimCounter;
import org.chuma.homecontroller.controller.actor.IOnOffActor;

/**
 * Action switching actor on and off after specified timeout.
 * It switches actor ON only if:
 * - it is OFF
 * - it was switched ON by any instance of AbstractSwitchOnActionWithTimer with the same or lower priority
 * - the action has Priority.HIGH.
 */
public class AbstractSwitchOnActionWithTimer<A extends IOnOffActor> extends AbstractAction<A> {
    public static final int BLINK_DELAY_MS = 600;
    public static final int MAX_BLINK_DURATION_MS = 10_000;
    static Logger log = LoggerFactory.getLogger(AbstractSwitchOnActionWithTimer.class.getName());

    private final Priority priority;
    private final ICondition condition;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    int timeoutMs;
    boolean canSwitchOn;

    /**
     * @param timeoutSec  switch actor of after this time in seconds
     * @param canSwitchOn is switching on allowed or just switch off after timeout is expected
     * @param priority    of this action
     * @param condition   optional condition to disable this action
     */
    public AbstractSwitchOnActionWithTimer(A actor, int timeoutSec, boolean canSwitchOn, Priority priority, ICondition condition) {
        super(actor);
        this.priority = priority;
        this.condition = condition;
        this.timeoutMs = timeoutSec * 1000;
        this.canSwitchOn = canSwitchOn;
    }

    private boolean canOverwriteState(IOnOffActor act) {
        return priority == Priority.HIGH
                || (act.getActionData() instanceof ActionData
                && ((ActionData)act.getActionData()).priority.ordinal() <= priority.ordinal());
    }

    @Override
    public void perform(int timeSinceLastAction) {
        if (condition != null) {
            if (condition.isTrue(timeSinceLastAction)) {
                log.debug("doing action - condition " + condition + " is true");
            } else {
                log.debug("no action - condition " + condition + " is false");
                return;
            }
        }
        // run body in extra thread because it can be blocking
        executor.execute(this::performImpl);
    }

    private void performImpl() {
        log.debug("Performing actor: " + actor.toString());
        try {
            ActionData aData = new ActionData(priority);
            synchronized (actor) {
                if (actor.isOn() && !canOverwriteState(actor)) {
                    log.debug("switched on, but by different action type -> do not touch anything");
                    return;
                }
                if (!actor.isOn() && !canSwitchOn) {
                    log.debug("already switched off, nothing to do for canSwitchOn == false");
                    return;
                }

                long endTime = System.currentTimeMillis() + timeoutMs;

                if (canSwitchOn) {
                    log.debug("switching on, setting my data");
                    switchOnImpl(aData);
                } else {
                    log.debug("cannot switch on, setting my data only");
                    actor.setActionData(aData);
                }

                // wait to start blinking
                if (timeoutMs > MAX_BLINK_DURATION_MS) {
                    log.debug("Going to wait for {} ms", timeoutMs - MAX_BLINK_DURATION_MS);
                    actor.wait(timeoutMs - MAX_BLINK_DURATION_MS);
                    log.debug("Woken up");
                }
                if (actor.getActionData() != aData) {
                    log.debug("action data modified by other thread, leaving action");
                    return;
                }

                // wait in the loop with blinking to switch actor off after specified timeout
                while (System.currentTimeMillis() < endTime) {
                    aData.incrementCount();
                    actor.callListenersAndSetActionData(aData);
                    long remains = endTime - System.currentTimeMillis();
                    if (remains > 0) {
                        actor.wait((remains < BLINK_DELAY_MS) ? remains : BLINK_DELAY_MS);
                    }
                    if (actor.getActionData() != aData) {
                        // there was some external modification
                        return;
                    }
                }

                log.debug("nobody modified actor meanwhile -> can switch off");
                actor.switchOff(null);
            }
        } catch (Exception e) {
            log.error("perform() method failed", e);
        }
    }

    protected void switchOnImpl(ActionData aData) {
        actor.switchOn(aData);
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