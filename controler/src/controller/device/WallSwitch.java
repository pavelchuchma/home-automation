package controller.device;

import controller.actor.Indicator;
import node.Node;
import node.NodePin;
import node.Pin;

public class WallSwitch extends ConnectedDevice {
    public WallSwitch(String id, Node node, int connectorPosition) {
        super(id, node, connectorPosition, new String[]{"btn1", "btn2", "btn3", "btn4", "greenLed", "redLed"});
    }

    public NodePin getButton1() {
        return pins[0];
    }

    public NodePin getButton2() {
        return pins[1];
    }

    public NodePin getButton3() {
        return pins[2];
    }

    public NodePin getButton4() {
        return pins[3];
    }

    public NodePin getGreenLed() {
        return pins[4];
    }

    public NodePin getRedLed() {
        return pins[5];
    }

    public Indicator getGreenLedIndicator(final boolean isInverted) {
        return new SwitchIndicator(getGreenLed(), isInverted);
    }

    public Indicator getRedLedIndicator(final boolean isInverted) {
        return new SwitchIndicator(getRedLed(), isInverted);
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
}