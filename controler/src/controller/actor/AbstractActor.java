package controller.actor;

import node.Pin;

public abstract class AbstractActor implements Actor{
    String id;
    int nodeId;
    Pin pin;
    int initValue;

    public AbstractActor(String id, int nodeId, Pin pin, int initValue) {
        this.id = id;
        this.nodeId = nodeId;
        this.pin = pin;
        this.initValue = initValue;
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

    public int getInitValue() {
        return initValue;
    }

    public abstract int getPinOutputMask();
}