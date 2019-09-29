package controller.action;

import controller.action.condition.ICondition;
import controller.actor.IOnOffActor;

public class SwitchOffAction extends AbstractAction {
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
    public void perform(int previousDurationMs) {
        if (condition != null && !condition.isTrue(previousDurationMs)) {
            return;
        }

        IOnOffActor actor = (IOnOffActor) getActor();
        if (actor.isOn()) {
            actor.switchOff(null);
        }
    }
}