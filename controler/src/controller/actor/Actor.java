package controller.actor;

import node.NodePin;

public interface Actor {
    public String getId();

    public abstract NodePin[] getOutputPins();

    public void perform();
}