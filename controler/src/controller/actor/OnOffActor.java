package controller.actor;

import node.NodePin;
import org.apache.log4j.Logger;

public class OnOffActor extends AbstractActor implements IOnOffActor {
    static Logger log = Logger.getLogger(OnOffActor.class.getName());

    int onValue;

    public OnOffActor(String id, String label, NodePin output, int initValue, int onValue, Indicator... indicators) {
        super(id, label, output, initValue, indicators);
        this.onValue = onValue;
    }

    @Override
    public synchronized boolean setValue(int val, Object actionData) {
        this.actionData = actionData;
        notifyAll();

        if (setPinValue(output, val, RETRY_COUNT)) {
            value = val;
            setIndicatorsAndActionData(false, actionData);
            return true;
        }
        return false;
    }

    public boolean switchOn(int percent, Object actionData) {
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