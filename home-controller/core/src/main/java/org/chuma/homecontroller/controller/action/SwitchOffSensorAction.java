package org.chuma.homecontroller.controller.action;

import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class SwitchOffSensorAction extends AbstractSensorAction {
    public SwitchOffSensorAction(IOnOffActor actor, int timeout, Priority priority) {
        super(actor, timeout, false, 0, priority, null);
    }
    public SwitchOffSensorAction(IOnOffActor actor, int timeout) {
        this(actor, timeout, Priority.LOW);
    }
}