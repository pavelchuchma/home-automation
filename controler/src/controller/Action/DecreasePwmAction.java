package controller.Action;

import controller.actor.PwmActor;

public class DecreasePwmAction extends AbstractAction {
    public DecreasePwmAction(PwmActor actor) {
        super(actor);
    }

    @Override
    public void perform() {
        ((PwmActor)getActor()).decreasePwm(1);
    }
}