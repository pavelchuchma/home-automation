package controller.Action;

import controller.actor.OnOffActor;

public class SwitchOnAction extends AbstractAction {
    public SwitchOnAction(OnOffActor actor) {
        super(actor);
    }

    @Override
    public void perform() {
        ((OnOffActor) getActor()).switchOn(null);
    }
}