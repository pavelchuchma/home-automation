package controller.action;

import controller.actor.Actor;
import controller.actor.ActorListener;
import controller.actor.IReadableOnOff;

public class IndicatorAction implements IReadableOnOff {
    private final ActorListener indicator;
    private boolean isOn;

    private Action onAction = new ActionImpl(true);
    private Action offAction = new ActionImpl(false);

    public IndicatorAction() {
        indicator = null;
    }

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

    private class ActionImpl implements Action {
        private final boolean value;

        private ActionImpl(boolean value) {
            this.value = value;
        }

        @Override
        public void perform(int previousDurationMs) {
            isOn = value;
            if (indicator != null) {
                indicator.onAction(IndicatorAction.this, false);
            }
        }

        @Override
        public Actor getActor() {
            return null;
        }
    }

}
