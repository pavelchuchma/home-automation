package org.chuma.homecontroller.controller.action;

import org.chuma.homecontroller.controller.action.condition.ICondition;
import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class SwitchOnActionWithTimer extends AbstractSwitchOnActionWithTimer<IOnOffActor> {
    public SwitchOnActionWithTimer(IOnOffActor actor, int timeoutSec) {
        this(actor, timeoutSec, null);
    }

    public SwitchOnActionWithTimer(IOnOffActor actor, int timeoutSec, ICondition condition) {
        super(actor, timeoutSec, true, Priority.LOW, condition);
    }

    public SwitchOnActionWithTimer(IOnOffActor actor, int timeoutSec, Priority priority, ICondition condition) {
        super(actor, timeoutSec, true, priority, condition);
    }
}