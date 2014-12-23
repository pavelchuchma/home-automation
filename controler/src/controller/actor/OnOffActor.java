package controller.actor;

import node.NodePin;
import org.apache.log4j.Logger;

public class OnOffActor extends AbstractActor implements IOnOffActor {
    static Logger log = Logger.getLogger(OnOffActor.class.getName());

    int onValue;

    OnOffActor conflictingActor;

    public OnOffActor(String id, NodePin output, int initValue, int onValue, Indicator... indicators) {
        super(id, output, initValue, indicators);
        this.onValue = onValue;
    }

    public void setConflictingActor(OnOffActor conflictingActor) {
        this.conflictingActor = conflictingActor;
    }

    @Override
    public synchronized boolean setValue(int val, Object actionData) {
        this.actionData = actionData;
        notifyAll();

        // turn off conflicting actor if turning on
        // necessary for louvers
        if (conflictingActor != null && val == onValue) {
            if (!conflictingActor.switchOff(actionData)) {
                log.error(String.format("Actor %s: Cannot switch off conflicting actor %s", getId(), conflictingActor.getId()));
                return false;
            }
        }

        if (setPinValue(output, val, RETRY_COUNT)) {
            value = val;
            setIndicatorsAndActionData(false, actionData);
            return true;
        }
        return false;
    }

    public boolean switchOn(Object actionData) {
        log.debug("switchOn: " + toString());
        return setValue(onValue, actionData);
    }

    public boolean switchOff(Object actionData) {
        log.debug("switchOff: " + toString());
        return setValue((onValue ^ 1) & 1, actionData);
    }

    public boolean isOn() {
        return value == onValue;
    }
}