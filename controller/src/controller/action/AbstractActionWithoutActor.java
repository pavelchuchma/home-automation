package controller.action;

import controller.actor.Actor;

public abstract class AbstractActionWithoutActor implements Action {
    @Override
    public Actor getActor() {
        return null;
    }

    public String toString() {
        return String.format("%s@%s", getClass().getName(), Integer.toHexString(System.identityHashCode(this)));
    }
}