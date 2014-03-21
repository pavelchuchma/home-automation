package controller.actor;

import node.NodePin;

public interface Actor {
    String getId();

    //abstract NodePin[] getOutputPins();

    void setValue(int val, Object actionData);

    Object getLastActionData();

    int getValue();
}