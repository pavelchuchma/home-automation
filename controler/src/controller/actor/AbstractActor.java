package controller.actor;

import node.NodePin;

public abstract class AbstractActor implements Actor {
    String id;
    NodePin output;
    int initValue;

    public AbstractActor(String id, NodePin output, int initValue) {
        this.id = id;
        this.output = output;
        this.initValue = initValue;
    }

    public String getId() {
        return id;
    }

    public int getInitValue() {
        return initValue;
    }
}