package controller.action;

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

    public String toString() {
        return String.format("%s@%s(%s)", getClass().getName(), Integer.toHexString(System.identityHashCode(this)), actor.getId());
    }
}