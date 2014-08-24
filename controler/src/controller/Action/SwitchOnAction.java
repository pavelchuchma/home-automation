package controller.Action;

import controller.actor.IOnOffActor;

public class SwitchOnAction extends AbstractAction {
    public SwitchOnAction(IOnOffActor actor) {
        super(actor);
    }

    @Override
    public void perform() {
        ((IOnOffActor) getActor()).switchOn(null);
    }
}