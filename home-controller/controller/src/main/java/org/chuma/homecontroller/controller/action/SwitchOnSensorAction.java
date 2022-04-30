package org.chuma.homecontroller.controller.action;

import org.chuma.homecontroller.controller.action.condition.ICondition;
import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class SwitchOnSensorAction extends AbstractSensorAction {

    public SwitchOnSensorAction(IOnOffActor actor, int timeoutSec) {
        this(actor, timeoutSec, 1d, Priority.LOW, null);
    }

    public SwitchOnSensorAction(IOnOffActor actor, int timeoutSec, double switchOnValue) {
        this(actor, timeoutSec, switchOnValue, Priority.LOW, null);
    }

    public SwitchOnSensorAction(IOnOffActor actor, int timeoutSec, double switchOnValue, Priority priority) {
        this(actor, timeoutSec, switchOnValue, priority, null);
    }

    public SwitchOnSensorAction(IOnOffActor actor, int timeoutSec, double switchOnValue, ICondition condition) {
        this(actor, timeoutSec, switchOnValue, Priority.LOW, condition);
    }

    public SwitchOnSensorAction(IOnOffActor actor, int timeoutSec, double switchOnValue, Priority priority, ICondition condition) {
        super(actor, timeoutSec, true, switchOnValue, priority, condition);
    }

    @Override
    public void perform(int timeSinceLastAction) {
        super.perform(timeSinceLastAction);
    }
}