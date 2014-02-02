package controller.actor;

import node.Pin;

public interface Actor {
    public String getId();
    public int getNodeId();
    public Pin getPin();
    public int getInitValue();
    public abstract int getPinOutputMask();

    public void perform();
}