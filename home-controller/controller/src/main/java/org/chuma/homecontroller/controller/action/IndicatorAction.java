package org.chuma.homecontroller.controller.action;

import org.chuma.homecontroller.controller.actor.ActorListener;
import org.chuma.homecontroller.controller.actor.IReadableOnOff;

public class IndicatorAction implements IReadableOnOff {
    private final ActorListener indicator;
    private boolean isOn;

    private final Action onAction = new ActionImpl(true);
    private final Action offAction = new ActionImpl(false);

    public IndicatorAction(ActorListener indicator) {
        this.indicator = indicator;
        indicator.addSource(this);
    }

    public Action getOnAction() {
        return onAction;
    }

    public Action getOffAction() {
        return offAction;
    }

    @Override
    public boolean isOn() {
        return isOn;
    }

    private class ActionImpl extends AbstractActionWithoutActor {
        private final boolean value;

        private ActionImpl(boolean value) {
            this.value = value;
        }

        @Override
        public void perform(int timeSinceLastAction) {
            isOn = value;
            indicator.onAction(IndicatorAction.this, null);
        }
    }
}
