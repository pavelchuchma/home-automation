package controller.device;

import controller.actor.Indicator;
import node.NodePin;

public class SwitchIndicator implements Indicator {
    NodePin pin;
    boolean isInverted;

    public SwitchIndicator(NodePin pin, boolean inverted) {
        this.pin = pin;
        isInverted = inverted;
    }

    @Override
    public NodePin getPin() {
        return pin;
    }

    @Override
    public boolean IsInverted() {
        return isInverted;
    }
}