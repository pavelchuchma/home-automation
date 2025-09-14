package org.chuma.homecontroller.controller.action;

import org.chuma.homecontroller.controller.action.condition.ICondition;
import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class SwitchOnActionWithTimer extends AbstractSwitchOnActionWithTimer<IOnOffActor> {
    public SwitchOnActionWithTimer(IOnOffActor actor, int timeoutSec) {
        this(actor, timeoutSec, 0);
    }

    public SwitchOnActionWithTimer(IOnOffActor actor, int timeoutSec, int timeoutMs) {
        super(actor, timeoutSec * 1000 + timeoutMs, true, Priority.LOW, null);
    }

    public SwitchOnActionWithTimer(IOnOffActor actor, int timeoutSec, ICondition condition) {
        super(actor, timeoutSec * 1000, true, Priority.LOW, condition);
    }

    public SwitchOnActionWithTimer(IOnOffActor actor, int timeoutSec, Priority priority, ICondition condition) {
        super(actor, timeoutSec * 1000, true, priority, condition);
    }
}