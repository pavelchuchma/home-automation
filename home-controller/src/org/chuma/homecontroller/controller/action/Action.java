package org.chuma.homecontroller.controller.action;

import org.chuma.homecontroller.controller.actor.Actor;

public interface Action {
    void perform(int previousDurationMs);

    Actor getActor();
}
