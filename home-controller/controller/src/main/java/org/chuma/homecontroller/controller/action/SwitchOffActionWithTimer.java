package org.chuma.homecontroller.controller.action;

import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class SwitchOffActionWithTimer extends AbstractSwitchOnActionWithTimer<IOnOffActor> {
    public SwitchOffActionWithTimer(IOnOffActor actor, int timeout, Priority priority) {
        super(actor, timeout, false, priority, null);
    }

    public SwitchOffActionWithTimer(IOnOffActor actor, int timeout) {
        this(actor, timeout, Priority.LOW);
    }
}