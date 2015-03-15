package controller.Action;

import controller.actor.IOnOffActor;
import org.apache.log4j.Logger;

public class AbstractSensorAction extends AbstractAction {
    public enum Priority {
        LOW,
        MEDIUM,
        HIGH
    }
    static Logger log = Logger.getLogger(AbstractSensorAction.class.getName());

    public static final int BLINK_DELAY = 600;
    public static final int MAX_BLINK_DURATION = 10000;
    private final int switchOnPercent;
    private final Priority priority;

    static class ActionData {
        private final Priority priority;

        ActionData(Priority priority) {
            this.priority = priority;
        }
    }

    int timeout;
    boolean canSwitchOn;

    protected AbstractSensorAction(IOnOffActor actor, int timeout, boolean canSwitchOn, int switchOnPercent, Priority priority) {
        super(actor);
        this.priority = priority;
        this.timeout = timeout * 1000;
        this.canSwitchOn = canSwitchOn;
        this.switchOnPercent = switchOnPercent;
    }

    private  boolean canOverwriteState(IOnOffActor act) {
        Object actionData = act.getLastActionData();
        return actionData != null
                && actionData.getClass() == ActionData.class
                && ((ActionData)actionData).priority.ordinal() <= priority.ordinal();
    }

    @Override
    public void perform(int previousDurationMs) {
        // run body in extra thread because it can be blocking
        new Thread(new Runnable() {
            @Override
            public void run() {
                performImpl();
            }
        }).start();
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
                if (act.getLastActionData() != aData) {
                    log.debug("action data modified by other thread, leaving action");
                    return;
                }

                boolean invert = true;
                while (System.currentTimeMillis() < endTime) {
                    act.setIndicatorsAndActionData(invert, aData);
                    invert = !invert;
                    long remains = endTime - System.currentTimeMillis();
                    if (remains > 0) {
                        act.wait((remains < BLINK_DELAY) ? remains : BLINK_DELAY);
                    }
                    if (act.getLastActionData() != aData) {
                        // there was some external modification
                        return;
                    }
                }

                log.debug("nobody modified actor meanwhile -> can switch off");
                act.switchOff(null);
            }
        } catch (Exception e) {
            log.error("perform() method failed2", e);
        }
    }
}