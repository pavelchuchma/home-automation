package controller.action;

import controller.actor.Actor;
import controller.actor.IOnOffActor;

public class InvertAction extends AbstractAction {

    private int switchOnPercent;

    public InvertAction(IOnOffActor actor) {
        this(actor, 100);
    }

    public InvertAction(Actor actor, int switchOnPercent) {
        super(actor);
        this.switchOnPercent = switchOnPercent;
    }

    @Override
    public void perform(int previousDurationMs) {
        IOnOffActor onOffActor = (IOnOffActor) actor;
        if (onOffActor.isOn()) {
            onOffActor.switchOff(null);
        }   else {
            onOffActor.switchOn(switchOnPercent, null);
        }
    }
}