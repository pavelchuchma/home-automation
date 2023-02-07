package org.chuma.homecontroller.controller.action;

import org.chuma.homecontroller.controller.action.condition.ICondition;
import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class SwitchOffAction extends AbstractAction<IOnOffActor> {
    private final ICondition condition;

    public SwitchOffAction(IOnOffActor actor) {
        super(actor);
        condition = null;
    }

    public SwitchOffAction(IOnOffActor actor, ICondition condition) {
        super(actor);
        this.condition = condition;
    }

    @Override
    public void perform(int timeSinceLastAction) {
        if (condition != null && !condition.isTrue(timeSinceLastAction)) {
            return;
        }

        if (actor.isOn()) {
            actor.switchOff(null);
        }
    }
}