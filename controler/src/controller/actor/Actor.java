package controller.actor;

import node.NodePin;

public interface Actor {
    String getId();

    //abstract NodePin[] getOutputPins();

    boolean setValue(int val, Object actionData);

    Object getLastActionData();

    public void removeActionData();

    int getValue();
}