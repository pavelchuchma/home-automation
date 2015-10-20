package controller.Action;

import controller.actor.Actor;

public interface Action {
    public void perform(int previousDurationMs);

    public Actor getActor();
}
