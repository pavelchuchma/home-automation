package org.chuma.homecontroller.controller.action;

import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class SwitchOffSensorAction extends AbstractSensorAction<IOnOffActor> {
    public SwitchOffSensorAction(IOnOffActor actor, int timeout, Priority priority) {
        super(actor, timeout, false, priority, null);
    }

    public SwitchOffSensorAction(IOnOffActor actor, int timeout) {
        this(actor, timeout, Priority.LOW);
    }
}