package controller.Action;

import controller.actor.IOnOffActor;

public class SwitchOffSensorAction extends AbstractSensorAction {
    public SwitchOffSensorAction(IOnOffActor actor, int timeout) {
        super(actor, timeout, false);
    }
}