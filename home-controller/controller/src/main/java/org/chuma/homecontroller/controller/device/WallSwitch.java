package org.chuma.homecontroller.controller.device;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.NodePin;
import org.chuma.homecontroller.base.node.OutputNodePin;

/**
 * Device consisting of four buttons (input) and two LEDs (lights, output).
 */
public class WallSwitch extends AbstractConnectedDevice {

    private static final String[] PIN_NAMES = new String[]{"btn1", "btn2", "btn3", "btn4", "greenLed", "redLed"};
    private SwitchIndicator redIndicator;
    private SwitchIndicator greenIndicator;

    public WallSwitch(String id, Node node, int connectorPosition) {
        super(id, node, connectorPosition, false);

        createPins(PIN_NAMES);
        finishInit();
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

    public OutputNodePin getGreenLed() {
        return (OutputNodePin)pins[4];
    }

    public OutputNodePin getRedLed() {
        return (OutputNodePin)pins[5];
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
    protected byte getDevicePinOutputMask() {
        return 0b0011_0000;
    }

    @Override
    public int getEventMask() {
        return createMask(getLeftUpperButton(), getLeftBottomButton(), getRightUpperButton(), getRightBottomButton());
    }

    public enum Side {
        LEFT,
        RIGHT
    }
}