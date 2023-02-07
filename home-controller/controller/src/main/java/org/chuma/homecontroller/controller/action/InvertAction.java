package org.chuma.homecontroller.controller.action;

import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class InvertAction extends AbstractAction<IOnOffActor> {
    public InvertAction(IOnOffActor actor) {
        super(actor);
    }

    @Override
    public void perform(int timeSinceLastAction) {
        if (actor.isOn()) {
            actor.switchOff(null);
        } else {
            actor.switchOn(null);
        }
    }
}