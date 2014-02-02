package controller;

import controller.actor.Actor;
import node.Pin;

public class Switch {
    String id;
    int nodeId;
    Pin pin;

    Actor[] buttonDownActors;
    Actor[] buttonUpActors;

    public Switch(String id, int nodeId, Pin pin, Actor[] buttonDownActors, Actor[] buttonUpActors) {
        this.id = id;
        this.nodeId = nodeId;
        this.pin = pin;
        this.buttonDownActors = buttonDownActors;
        this.buttonUpActors = buttonUpActors;
    }

    public String getId() {
        return id;
    }

    public int getNodeId() {
        return nodeId;
    }

    public Pin getPin() {
        return pin;
    }

    public Actor[] getButtonDownActors() {
        return buttonDownActors;
    }

    public Actor[] getButtonUpActors() {
        return buttonUpActors;
    }
}