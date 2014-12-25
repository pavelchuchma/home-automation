package controller.Action;

import controller.actor.PwmActor;

public class IncreasePwmAction extends AbstractAction {
    public IncreasePwmAction(PwmActor actor) {
        super(actor);
    }

    @Override
    public void perform(int previousDurationMs) {
        PwmActor actor = (PwmActor) getActor();
        int value = actor.getValue();

        int step;
        if (value == 0) {
            step = 1;
        } else if (value < 25) {
            step = 25 - value;
        } else if (value < 50) {
            step = 25;
        } else {
            step = 10;
        }
        actor.increasePwm(step, this);
    }
}