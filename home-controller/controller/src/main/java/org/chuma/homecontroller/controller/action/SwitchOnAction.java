package org.chuma.homecontroller.controller.action;

import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class SwitchOnAction extends AbstractAction {
    public SwitchOnAction(IOnOffActor actor) {
        super(actor);
    }

    @Override
    public void perform(int previousDurationMs) {
        ((IOnOffActor) getActor()).switchOn(null);
    }
}