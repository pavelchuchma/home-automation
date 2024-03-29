package org.chuma.homecontroller.controller.action;

import org.apache.commons.lang3.Validate;

import org.chuma.homecontroller.controller.action.condition.ICondition;
import org.chuma.homecontroller.controller.actor.IContinuousValueActor;

public class ContinuousValueSwitchOnActionWithTimer extends AbstractSwitchOnActionWithTimer<IContinuousValueActor> {
    private final double switchOnValue;

    public ContinuousValueSwitchOnActionWithTimer(IContinuousValueActor actor, int timeoutSec, double switchOnValue) {
        this(actor, timeoutSec, switchOnValue, Priority.LOW, null);
    }

    public ContinuousValueSwitchOnActionWithTimer(IContinuousValueActor actor, int timeoutSec, double switchOnValue, ICondition condition) {
        this(actor, timeoutSec, switchOnValue, Priority.LOW, condition);
    }

    public ContinuousValueSwitchOnActionWithTimer(IContinuousValueActor actor, int timeoutSec, double switchOnValue, Priority priority, ICondition condition) {
        super(actor, timeoutSec, true, priority, condition);
        this.switchOnValue = switchOnValue;
        Validate.inclusiveBetween(0, 1, switchOnValue);
    }

    @Override
    protected void switchOnImpl(ActionData aData) {
        actor.switchOn(switchOnValue, aData);
    }
}