package controller.action;

import controller.actor.IOnOffActor;

public class SwitchOnSensorAction extends AbstractSensorAction {

    public SwitchOnSensorAction(IOnOffActor actor, int timeout, int switchOnPercent) {
        this(actor, timeout, switchOnPercent, Priority.LOW, null);
    }

    public SwitchOnSensorAction(IOnOffActor actor, int timeout, int switchOnPercent, Priority priority) {
        this(actor, timeout, switchOnPercent, priority, null);
    }

    public SwitchOnSensorAction(IOnOffActor actor, int timeout, int switchOnPercent, ICondition condition) {
        this(actor, timeout, switchOnPercent, Priority.LOW, condition);
    }

    public SwitchOnSensorAction(IOnOffActor actor, int timeout, int switchOnPercent, Priority priority, ICondition condition) {
        super(actor, timeout, true, switchOnPercent, priority, condition);
    }

    @Override
    public void perform(int previousDurationMs) {
        super.perform(previousDurationMs);
    }
}