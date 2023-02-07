package org.chuma.homecontroller.controller.action;

import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class SwitchOnAction extends AbstractAction<IOnOffActor> {
    public SwitchOnAction(IOnOffActor actor) {
        super(actor);
    }

    @Override
    public void perform(int timeSinceLastAction) {
        actor.switchOn();
    }
}