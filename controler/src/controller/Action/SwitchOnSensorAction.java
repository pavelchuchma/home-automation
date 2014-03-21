package controller.Action;

import controller.actor.OnOffActor;

public class SwitchOnSensorAction extends AbstractSensorAction {
    public SwitchOnSensorAction(OnOffActor actor, int timeout) {
        super(actor, timeout, false);
    }
}