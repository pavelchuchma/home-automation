package controller.Action;

import controller.actor.IOnOffActor;

public class SwitchOffAction extends AbstractAction {
    public SwitchOffAction(IOnOffActor actor) {
        super(actor);
    }

    @Override
    public void perform(int previousDurationMs) {
        ((IOnOffActor) getActor()).switchOff(null);
    }
}