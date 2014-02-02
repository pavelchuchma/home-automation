package controller;

import node.Pin;

public class Switch {
    String id;
    int nodeId;
    Pin pin;

    Action[] buttonDownActions;
    Action[] buttonUpActions;

    public Switch(String id, int nodeId, Pin pin, Action[] buttonDownActions, Action[] buttonUpActions) {
        this.id = id;
        this.nodeId = nodeId;
        this.pin = pin;
        this.buttonDownActions = buttonDownActions;
        this.buttonUpActions = buttonUpActions;
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

    public Action[] getButtonDownActions() {
        return buttonDownActions;
    }

    public Action[] getButtonUpActions() {
        return buttonUpActions;
    }
}