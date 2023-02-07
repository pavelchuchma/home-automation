package org.chuma.homecontroller.controller.action;

import org.chuma.homecontroller.controller.actor.IOnOffActor;
import org.chuma.homecontroller.controller.actor.OnOffActor;

public class InvertActionWithTimer extends AbstractSensorAction<IOnOffActor> {
    public InvertActionWithTimer(OnOffActor actor, int timeoutSec) {
        super(actor, timeoutSec, true, Priority.LOW, null);
    }

    @Override
    public void perform(int timeSinceLastAction) {
        if (actor.isOn()) {
            // is on -> switch off
            actor.switchOff(null);
        } else {
            // is off -> switch on with timer
            super.perform(timeSinceLastAction);
        }
    }
}