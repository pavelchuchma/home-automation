package org.chuma.homecontroller.controller.device;

import org.apache.commons.lang3.Validate;

import org.chuma.homecontroller.base.node.CpuFrequency;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.NodePin;
import org.chuma.homecontroller.base.node.Pin;
import org.chuma.homecontroller.base.node.PwmOutputNodePin;

/**
 * Device consisting of four buttons (input) and two LEDs (lights, output).
 */
public class WallSwitch extends AbstractConnectedDevice {

    private static final String[] PIN_NAMES = new String[]{"btn1", "btn2", "btn3", "btn4", "greenLed", "redLed"};
    private SwitchIndicator redIndicator;
    private SwitchIndicator greenIndicator;
    private final double indicatorIntensity;

    public WallSwitch(String id, Node node, int connectorPosition) {
        this(id, node, connectorPosition, 1);
    }

    public WallSwitch(String id, Node node, int connectorPosition, double indicatorIntensity) {
        super(id, node, connectorPosition, false, (indicatorIntensity != 1) ? CpuFrequency.sixteenMHz : CpuFrequency.unknown);
        this.indicatorIntensity = indicatorIntensity;
        Validate.inclusiveBetween(0.01, 1, indicatorIntensity);
        createPins(PIN_NAMES);
        finishInit();
    }

    @Override
    protected PwmOutputNodePin createOutputNodePin(String pinId, String name, Node node, Pin pin) {
        return new PwmOutputNodePin(pinId, name, node, pin, 48);
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

    public PwmOutputNodePin getGreenLed() {
        return (PwmOutputNodePin)pins[4];
    }

    public PwmOutputNodePin getRedLed() {
        return (PwmOutputNodePin)pins[5];
    }

    public SwitchIndicator getGreenLedIndicator(final SwitchIndicator.Mode mode) {
        if (greenIndicator == null) {
            greenIndicator = new SwitchIndicator(getGreenLed(), mode, indicatorIntensity);
        } else if (greenIndicator.mode != mode) {
            throw new IllegalArgumentException("Getting indicator in different mode than before");
        }
        return greenIndicator;
    }

    public SwitchIndicator getRedLedIndicator(final SwitchIndicator.Mode mode) {
        if (redIndicator == null) {
            redIndicator = new SwitchIndicator(getRedLed(), mode, indicatorIntensity);
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