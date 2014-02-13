package controller.actor;

import node.NodePin;

public interface Indicator {
    NodePin getPin();
    boolean IsInverted();
}
