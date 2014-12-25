package controller.Action;

import controller.actor.IOnOffActor;
import org.apache.log4j.Logger;

public class AbstractSensorAction extends AbstractAction {

    static Logger log = Logger.getLogger(AbstractSensorAction.class.getName());

    public static final int BLINK_DELAY = 600;
    public static final int MAX_BLINK_DURATION = 10000;

    class ActionData {
        ActionData() {
        }
    }

    int timeout;
    boolean canSwitchOn;

    protected AbstractSensorAction(IOnOffActor actor, int timeout, boolean canSwitchOn) {
        super(actor);
        this.timeout = timeout * 1000;
        this.canSwitchOn = canSwitchOn;
    }

    private static boolean isSensorActionData(IOnOffActor act) {
        Object actionData = act.getLastActionData();
        return actionData != null && actionData.getClass() == ActionData.class;
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
            ActionData aData = new ActionData();
            synchronized (act) {
                if (act.isOn() && !isSensorActionData(act)) {
                    log.debug("switched on, but by different action type -> do not touch anything");
                    return;
                }
                if (!act.isOn() && !canSwitchOn) {
                    log.error("already switched off, nothing to do for canSwitchOn == false");
                    return;
                }

                long endTime = System.currentTimeMillis() + timeout;

                log.debug("is switched off or switched on by my action type -> switch on, set my data");
                act.switchOn(aData);

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