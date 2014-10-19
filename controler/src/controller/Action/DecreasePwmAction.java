package controller.Action;

import controller.actor.PwmActor;

public class DecreasePwmAction extends AbstractAction {
    public DecreasePwmAction(PwmActor actor) {
        super(actor);
    }

    @Override
    public void perform() {
        int value = actor.getValue();
        int step;
        if (value == 0) {
            step = 1;
        } else if (value == 4) {
            step = 3;
        } else if (value == 8) {
            step = 4;
        } else if (value > 8) {
            step = value - 8;
        } else {
            step = 1;
        }
        ((PwmActor)getActor()).decreasePwm(step);
    }
}