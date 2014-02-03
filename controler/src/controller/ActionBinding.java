package controller;

import controller.actor.Actor;
import node.NodePin;

public class ActionBinding {
    NodePin trigger;

    Actor[] buttonDownActors;
    Actor[] buttonUpActors;

    public ActionBinding(NodePin trigger, Actor[] buttonDownActors, Actor[] buttonUpActors) {
        this.trigger = trigger;
        this.buttonDownActors = buttonDownActors;
        this.buttonUpActors = buttonUpActors;
    }

    public NodePin getTrigger() {
        return trigger;
    }

    public String toString() {
        return trigger.toString();
    }

    public Actor[] getButtonDownActors() {
        return buttonDownActors;
    }

    public Actor[] getButtonUpActors() {
        return buttonUpActors;
    }
}