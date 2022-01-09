package org.chuma.homecontroller.extensions.actor;

import org.chuma.homecontroller.controller.actor.AbstractActor;
import org.chuma.homecontroller.controller.actor.ActorListener;
import org.chuma.homecontroller.controller.actor.IOnOffActor;
import org.chuma.homecontroller.extensions.external.Recuperation;

public class RecuperationActor extends AbstractActor implements IOnOffActor {
    Recuperation recuperation = new Recuperation();

    public RecuperationActor(ActorListener... actorListeners) {
        super("Recuperation", "Rekuperace", actorListeners);
    }

    @Override
    public boolean switchOn(int percent, Object actionData) {
        return setValue(2, actionData);
    }

    @Override
    public boolean switchOff(Object actionData) {
        return setValue(0, null);
    }

    @Override
    public boolean setValue(int val, Object actionData) {
        if (recuperation.setSpeed((val == 0) ? 0 : 2)) {
            callListenersAndSetActionData(false, actionData);
            return true;
        }
        return false;
    }

    @Override
    public int getValue() {
        return recuperation.getStatus().getSpeed();
    }

    @Override
    public boolean isOn() {
        return getValue() != 0;
    }
}