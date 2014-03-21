package controller.Action;

import controller.actor.OnOffActor;

public class AbstractSensorAction extends AbstractAction {

    public static final int BLINK_DELAY = 600;
    public static final int MAX_BLINK_DURATION = 10000;

    class ActionData {
        ActionData() {
        }
    }

    int timeout;
    boolean switchOffOnly;

    protected AbstractSensorAction(OnOffActor actor, int timeout, boolean switchOffOnly) {
        super(actor);
        this.timeout = timeout * 1000;
        this.switchOffOnly = switchOffOnly;
    }

    private static boolean isSensorActionData(OnOffActor act) {
        Object actionData = act.getLastActionData();
        return actionData != null && actionData.getClass() == ActionData.class;
    }

    @Override
    public void perform() {
        OnOffActor act = (OnOffActor) getActor();
        try {
            ActionData aData = new ActionData();
            synchronized (act) {
                if (act.isOn() && !isSensorActionData(act)) {
                    // switched on, but by different action type -> do not touch anything
                    return;
                }
                if (switchOffOnly && !act.isOn()) {
                    // already switched off, nothing to do for switchOffOnly
                    return;
                }

                // is switched off or switched on by my action type -> switch on, set my data
                act.switchOn(aData);


                long endTime = System.currentTimeMillis() + timeout;

                if (timeout > MAX_BLINK_DURATION) {
                    act.wait(timeout - MAX_BLINK_DURATION);
                }
                if (act.getLastActionData() != aData) {
                    return;
                }

                boolean invert = true;
                while (System.currentTimeMillis() < endTime) {
                    act.setIndicators(invert, aData);
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

                // nobody modified actor meanwhile -> can switch off
                act.switchOff(null);
            }
        } catch (InterruptedException e) {
            return;
        }
    }
}