package controller.Action;

import controller.actor.PwmActor;

public class IncreasePwmAction extends AbstractAction {
    public IncreasePwmAction(PwmActor actor) {
        super(actor);
    }

    @Override
    public void perform() {
        ((PwmActor)getActor()).increasePwm(1);
    }
}