package org.chuma.homecontroller.controller.action;

import org.chuma.homecontroller.controller.action.condition.ICondition;
import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class SwitchOnSensorAction extends AbstractSensorAction<IOnOffActor> {
    public SwitchOnSensorAction(IOnOffActor actor, int timeoutSec) {
        this(actor, timeoutSec, null);
    }

    public SwitchOnSensorAction(IOnOffActor actor, int timeoutSec, ICondition condition) {
        super(actor, timeoutSec, true, Priority.LOW, condition);
    }
}