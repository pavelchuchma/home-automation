package controller.Action;

import controller.actor.IOnOffActor;

public class InvertAction extends AbstractAction {
    public InvertAction(IOnOffActor actor) {
        super(actor);
    }

    @Override
    public void perform(int previousDurationMs) {
        IOnOffActor onOffActor = (IOnOffActor) actor;
        if (onOffActor.isOn()) {
            onOffActor.switchOff(null);
        }   else {
            onOffActor.switchOn(null);
        }
    }
}