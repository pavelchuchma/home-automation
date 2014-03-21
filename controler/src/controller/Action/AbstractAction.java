package controller.Action;

import controller.actor.Actor;

public abstract class AbstractAction implements Action {
    protected Actor actor;

    public AbstractAction(Actor actor) {
        this.actor = actor;
    }

    @Override
    public Actor getActor() {
        return actor;
    }
}