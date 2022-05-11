package org.chuma.homecontroller.controller.action;

import org.chuma.homecontroller.controller.actor.ActorListener;
import org.chuma.homecontroller.controller.actor.IReadableOnOff;

public class IndicatorAction implements IReadableOnOff {
    private final Action onAction;
    private final Action offAction;
    private boolean isOn;

    public IndicatorAction(ActorListener indicator) {
        indicator.addSource(this);
        onAction = new GenericCodeAction(timeSinceLastAction -> performAction(indicator, true));
        offAction = new GenericCodeAction(timeSinceLastAction -> performAction(indicator, false));
    }

    private void performAction(ActorListener indicator, boolean value) {
        isOn = value;
        indicator.onAction(IndicatorAction.this, null);
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
}
