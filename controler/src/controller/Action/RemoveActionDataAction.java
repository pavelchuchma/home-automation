package controller.Action;

import controller.actor.PwmActor;

public class RemoveActionDataAction extends AbstractAction {
    public RemoveActionDataAction(PwmActor actor) {
        super(actor);
    }

    @Override
    public void perform() {
        getActor().removeActionData();
    }
}