package controller.device;

import node.NodePin;

public class WallSwitch extends ConnectedDevice {
    public WallSwitch(String id, int nodeId, int connectorPosition) {
        super(id, nodeId, connectorPosition, new String[]{"btn1", "btn2", "btn3", "btn4", "greenLed", "redLed"});
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
}