package controller.action;

import controller.actor.PwmActor;

public class DecreasePwmAction extends AbstractAction {
    public DecreasePwmAction(PwmActor actor) {
        super(actor);
    }

    @Override
    public void perform(int previousDurationMs) {
        int value = actor.getValue();
        int step;
        if (value == 0) {
            step = 1;
        } else if (value == 25) {
            step = 25 - 1;
        } else if (value == 50) {
            step = 25;
        } else if (value > 50) {
            step = value - 50;
        } else {
            step = 10;
        }
        ((PwmActor) getActor()).decreasePwm(step, this);
    }
}