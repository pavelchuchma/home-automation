package controller.action;

import controller.actor.IOnOffActor;

public class SwitchOnAction extends AbstractAction {
    public SwitchOnAction(IOnOffActor actor) {
        super(actor);
    }

    @Override
    public void perform(int previousDurationMs) {
        ((IOnOffActor) getActor()).switchOn(100, null);
    }
}