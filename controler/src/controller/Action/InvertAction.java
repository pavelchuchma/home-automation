package controller.Action;

import controller.actor.Actor;

public class InvertAction extends AbstractAction {
    public InvertAction(Actor actor) {
        super(actor);
    }

    @Override
    public void perform(int previousDurationMs) {
        getActor().setValue((actor.getValue() ^ 1) & 1, null);
    }
}