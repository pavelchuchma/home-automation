package controller.Action;

import controller.actor.Actor;

public interface Action {
    public void perform();

    public Actor getActor();
}
