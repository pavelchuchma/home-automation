package org.chuma.homecontroller.controller.action;

import org.chuma.homecontroller.controller.actor.Actor;

/**
 * Represents an action to do with a specified {@link Actor}.
 */
public interface Action {
    /**
     * Perform the action
     * @param previousDurationMs time since previous related action. For example: OnButtonUp gets duration of button down phase.
     */
    void perform(int previousDurationMs);

    Actor getActor();
}
