package org.chuma.homecontroller.controller.actor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.node.OutputNodePin;

public class OnOffActor extends AbstractPinActor implements IOnOffActor {
    static Logger log = LoggerFactory.getLogger(OnOffActor.class.getName());

    final int onValue;
    int value;

    public OnOffActor(String id, String label, OutputNodePin output, ActorListener... actorListeners) {
        super(id, label, output, actorListeners);
        this.onValue = (output.isHighValueMeansOn()) ? 1 : 0;
        this.value = (onValue ^ 1);
    }

    @Override
    public synchronized boolean setValue(double val, Object actionData) {
        final int newValue = (val == 0) ? 0 : 1;
        if (setPinValue(outputPin, newValue, RETRY_COUNT)) {
            this.value = newValue;
            callListenersAndSetActionData(actionData);
            return true;
        }
        return false;
    }

    @Override
    public double getValue() {
        return value;
    }

    public boolean switchOn(double value, Object actionData) {
        log.debug("switchOn: " + this);
        return setValue(onValue, actionData);
    }

    public boolean switchOff(Object actionData) {
        log.debug("switchOff: " + this);
        return setValue((onValue ^ 1) & 1, actionData);
    }

    public boolean isOn() {
        return value == onValue;
    }
}