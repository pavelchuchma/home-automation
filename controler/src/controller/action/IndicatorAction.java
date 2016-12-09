package controller.action;

import controller.actor.Actor;
import controller.actor.ActorListener;
import controller.actor.IReadableOnOff;

public class IndicatorAction {
    private final ActorListener indicator;
    private boolean isOn;
    private IReadableOnOff source = () -> isOn;

    private Action onAction = new ActionImpl(true);
    private Action offAction = new ActionImpl(false);

    public IndicatorAction(ActorListener indicator) {
        this.indicator = indicator;
        indicator.addSource(source);
    }

    public Action getOnAction() {
        return onAction;
    }

    public Action getOffAction() {
        return offAction;
    }

    private class ActionImpl implements Action {
        private final boolean value;

        private ActionImpl(boolean value) {
            this.value = value;
        }

        @Override
        public void perform(int previousDurationMs) {
            isOn = value;
            indicator.onAction(source, false);
        }

        @Override
        public Actor getActor() {
            return null;
        }
    }

}
