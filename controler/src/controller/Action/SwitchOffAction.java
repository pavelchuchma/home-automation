package controller.Action;

import controller.actor.OnOffActor;

public class SwitchOffAction extends AbstractAction {
    public SwitchOffAction(OnOffActor actor) {
        super(actor);
    }

    @Override
    public void perform() {
        ((OnOffActor) getActor()).switchOff(null);
    }
}