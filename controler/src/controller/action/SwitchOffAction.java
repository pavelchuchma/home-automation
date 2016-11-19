package controller.action;

import controller.actor.IOnOffActor;

public class SwitchOffAction extends AbstractAction {
    public SwitchOffAction(IOnOffActor actor) {
        super(actor);
    }

    @Override
    public void perform(int previousDurationMs) {
        IOnOffActor actor = (IOnOffActor) getActor();
        if (actor.isOn()) {
            actor.switchOff(null);
        }
    }
}