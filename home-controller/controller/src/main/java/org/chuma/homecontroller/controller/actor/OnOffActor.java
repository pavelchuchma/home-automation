package org.chuma.homecontroller.controller.actor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.node.OutputNodePin;

public class OnOffActor extends AbstractPinActor implements IOnOffActor {
    static Logger log = LoggerFactory.getLogger(OnOffActor.class.getName());

    private boolean isOn = false;

    public OnOffActor(String id, String label, OutputNodePin output, ActorListener... actorListeners) {
        super(id, label, output, actorListeners);
    }

    private synchronized boolean setValue(boolean val, Object actionData) {
        if (setPinValue(outputPin, val, RETRY_COUNT)) {
            this.isOn = val;
            callListenersAndSetActionData(actionData);
            return true;
        }
        return false;
    }

    @Override
    public boolean switchOn(Object actionData) {
        log.debug("switchOn: " + this);
        return setValue(true, actionData);
    }

    @Override
    public boolean switchOff(Object actionData) {
        log.debug("switchOff: " + this);
        return setValue(false, actionData);
    }

    @Override
    public boolean isOn() {
        return isOn;
    }
}