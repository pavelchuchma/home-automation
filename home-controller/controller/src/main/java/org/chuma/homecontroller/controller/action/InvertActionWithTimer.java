package org.chuma.homecontroller.controller.action;

import org.chuma.homecontroller.controller.actor.OnOffActor;

public class InvertActionWithTimer extends AbstractSensorAction {
    public InvertActionWithTimer(OnOffActor actor, int switchOffTimer) {
        super(actor, switchOffTimer, true, 1, Priority.LOW, null);
    }

    @Override
    public void perform(int timeSinceLastAction) {
        OnOffActor act = (OnOffActor) actor;
        if (act.isOn()) {
            // is on -> switch off
            act.switchOff(null);
        } else {
            // is off -> switch on with timer
            super.perform(timeSinceLastAction);
        }
    }
}