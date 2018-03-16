package controller.action;

import controller.actor.Actor;

public interface Action {
    void perform(int previousDurationMs);

    Actor getActor();
}
