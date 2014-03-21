package controller.Action;

import controller.actor.OnOffActor;

public class SwitchOffSensorAction extends AbstractSensorAction {
    public SwitchOffSensorAction(OnOffActor actor, int timeout) {
        super(actor, timeout, true);
    }
}