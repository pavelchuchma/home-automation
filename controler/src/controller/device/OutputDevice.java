package controller.device;

import node.NodePin;

public class OutputDevice extends ConnectedDevice {
    public OutputDevice(String id, int nodeId, int connectorPosition) {
        super(id, nodeId, connectorPosition, new String[]{"out1", "out2", "out3", "out4", "out5", "out6"});
    }

    public NodePin getOut1() {
        return pins[0];
    }

    public NodePin getOut2() {
        return pins[1];
    }

    public NodePin getOut3() {
        return pins[2];
    }

    public NodePin getOut4() {
        return pins[3];
    }

    public NodePin getOut5() {
        return pins[4];
    }

    public NodePin getOut6() {
        return pins[5];
    }
}