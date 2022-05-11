package org.chuma.homecontroller.controller.action;

import java.util.function.IntConsumer;

import org.chuma.homecontroller.controller.actor.Actor;

/**
 * A generic action accepting code instead of Actor.
 */
public class GenericCodeAction implements Action {
    private final IntConsumer code;

    public GenericCodeAction(IntConsumer code) {
        this.code = code;
    }

    @Override
    public void perform(int timeSinceLastAction) {
        code.accept(timeSinceLastAction);
    }

    @Override
    public Actor getActor() {
        return null;
    }

    public String toString() {
        return String.format("%s@%s", getClass().getName(), Integer.toHexString(System.identityHashCode(this)));
    }
}