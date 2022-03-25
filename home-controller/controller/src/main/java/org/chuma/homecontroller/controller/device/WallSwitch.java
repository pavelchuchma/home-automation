package org.chuma.homecontroller.controller.device;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.NodePin;

public class WallSwitch extends AbstractConnectedDevice {

    private SwitchIndicator redIndicator;
    private SwitchIndicator greenIndicator;

    public WallSwitch(String id, Node node, int connectorPosition) {
        super(id, node, connectorPosition, new String[]{"btn1", "btn2", "btn3", "btn4", "greenLed", "redLed"});
    }

    public NodePin getLeftUpperButton() {
        return pins[2];
    }

    public NodePin getLeftBottomButton() {
        return pins[3];
    }

    public NodePin getRightUpperButton() {
        return pins[1];
    }

    public NodePin getRightBottomButton() {
        return pins[0];
    }

    public NodePin getGreenLed() {
        return pins[4];
    }

    public NodePin getRedLed() {
        return pins[5];
    }

    public SwitchIndicator getGreenLedIndicator(final SwitchIndicator.Mode mode) {
        if (greenIndicator == null) {
            greenIndicator = new SwitchIndicator(getGreenLed(), mode);
        } else if (greenIndicator.mode != mode) {
            throw new IllegalArgumentException("Getting indicator in different mode than before");
        }
        return greenIndicator;
    }

    public SwitchIndicator getRedLedIndicator(final SwitchIndicator.Mode mode) {
        if (redIndicator == null) {
            redIndicator = new SwitchIndicator(getRedLed(), mode);
        } else if (redIndicator.mode != mode) {
            throw new IllegalArgumentException("Getting indicator in different mode than before");
        }
        return redIndicator;
    }

    @Override
    public int getEventMask() {
        return createMask(pins[0], pins[1], pins[2], pins[3]);
    }

    @Override
    public int getOutputMasks() {
        return createMask(getRedLed(), getGreenLed());
    }

    @Override
    public int getInitialOutputValues() {
        //TODO: initialize in accord with assigned indicators
        return createMask(getGreenLed(), getRedLed());
    }

    public enum Side {
        LEFT,
        RIGHT
    }
}