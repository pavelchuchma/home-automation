package controller.Action;

import controller.actor.OnOffActor;

public class InvertActionWithTimer extends AbstractSensorAction {
    public InvertActionWithTimer(OnOffActor actor, int switchOffTimer) {
        super(actor, switchOffTimer, false);
    }

    @Override
    public void perform() {
        OnOffActor act = (OnOffActor) actor;
        if (act.isOn()) {
            // is on -> switch off
            act.switchOff(null);
        } else {
            // is off -> switch on with timer
            super.perform();
        }
    }
}