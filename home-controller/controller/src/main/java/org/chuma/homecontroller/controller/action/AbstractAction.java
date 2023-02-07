package org.chuma.homecontroller.controller.action;

import org.chuma.homecontroller.controller.actor.Actor;

public abstract class AbstractAction<A extends Actor> implements Action {
    protected final A actor;

    public AbstractAction(A actor) {
        this.actor = actor;
    }

    @Override
    public A getActor() {
        return actor;
    }

    public String toString() {
        return String.format("%s@%s(%s)", getClass().getName(), Integer.toHexString(System.identityHashCode(this)), actor.getId());
    }
}