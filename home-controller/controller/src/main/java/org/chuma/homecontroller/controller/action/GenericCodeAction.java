package org.chuma.homecontroller.controller.action;

import java.util.function.IntConsumer;

import org.chuma.homecontroller.controller.actor.Actor;

/**
 * A generic action accepting code instead of Actor.
 */
public class GenericCodeAction implements Action {
    private final IntConsumer code;
    private final String name;

    public GenericCodeAction(IntConsumer code) {
        this(code, null);
    }

    /**
     *
     * @param code body of perform() method
     * @param name name for debug purposes only
     */
    public GenericCodeAction(IntConsumer code, String name) {
        this.code = code;
        this.name = name;
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
        StringBuilder s = new StringBuilder().append(getClass().getName()).append("@").append(Integer.toHexString(System.identityHashCode(this)));
        if (name != null) {
            s.append("(").append(name).append(")");
        }
        return s.toString();
    }
}