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
        } else if (value == PwmActor.MAX_PWM_VALUE / 4) {
            step = PwmActor.MAX_PWM_VALUE / 4 - 1;
        } else if (value == PwmActor.MAX_PWM_VALUE / 2) {
            step = PwmActor.MAX_PWM_VALUE / 4;
        } else if (value > PwmActor.MAX_PWM_VALUE / 2) {
            step = value - PwmActor.MAX_PWM_VALUE / 2;
        } else {
            step = PwmActor.MAX_PWM_VALUE / 10;
        }
        ((PwmActor) getActor()).decreasePwm(step);
    }
}