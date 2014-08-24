package controller.Action;

import controller.actor.IOnOffActor;

public class SwitchOffAction extends AbstractAction {
    public SwitchOffAction(IOnOffActor actor) {
        super(actor);
    }

    @Override
    public void perform() {
        ((IOnOffActor) getActor()).switchOff(null);
    }
}