package org.chuma.homecontroller.controller.device;

import org.chuma.homecontroller.nodes.node.Node;
import org.chuma.homecontroller.nodes.node.NodePin;
import org.chuma.homecontroller.nodes.node.Pin;

public class WallSwitch extends ConnectedDevice {

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
        return createMask(new Pin[]{pins[0].getPin(), pins[1].getPin(), pins[2].getPin(), pins[3].getPin()});
    }

    @Override
    public int getOutputMasks() {
        return createMask(new Pin[]{getRedLed().getPin(), getGreenLed().getPin()});
    }

    @Override
    public int getInitialOutputValues() {
        //TODO: initialize in accord with assigned indicators
        return createMask(new Pin[]{getGreenLed().getPin(), getRedLed().getPin()});
    }

    public enum Side {
        LEFT,
        RIGHT
    }
}