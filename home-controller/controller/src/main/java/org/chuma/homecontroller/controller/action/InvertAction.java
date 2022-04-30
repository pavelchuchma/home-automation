package org.chuma.homecontroller.controller.action;

import org.chuma.homecontroller.controller.actor.Actor;
import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class InvertAction extends AbstractAction {

    private final double switchOnValue;

    public InvertAction(IOnOffActor actor) {
        this(actor, 1.0);
    }

    public InvertAction(Actor actor, double switchOnValue) {
        super(actor);
        this.switchOnValue = switchOnValue;
    }

    @Override
    public void perform(int timeSinceLastAction) {
        IOnOffActor onOffActor = (IOnOffActor) actor;
        if (onOffActor.isOn()) {
            onOffActor.switchOff(null);
        } else {
            onOffActor.switchOn(switchOnValue, null);
        }
    }
}