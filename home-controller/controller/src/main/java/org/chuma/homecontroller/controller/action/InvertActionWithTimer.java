package org.chuma.homecontroller.controller.action;

import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class InvertActionWithTimer extends AbstractSwitchOnActionWithTimer<IOnOffActor> {
    public InvertActionWithTimer(IOnOffActor actor, int timeoutSec) {
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