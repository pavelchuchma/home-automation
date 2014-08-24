package controller.Action;

import controller.actor.IOnOffActor;

public class SwitchOnSensorAction extends AbstractSensorAction {
    public SwitchOnSensorAction(IOnOffActor actor, int timeout) {
        super(actor, timeout, false);
    }
}