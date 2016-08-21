package controller.device;

import node.Node;
import node.NodePin;
import node.Pin;

public class OutputDevice extends ConnectedDevice {

    private static final String[] PIN_NAMES = new String[]{"out1", "out2", "out3", "out4", "out5", "out6"};

    public OutputDevice(String id, Node node, int connectorPosition) {
        super(id, node, connectorPosition, PIN_NAMES);
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

    @Override
    public int getEventMask() {
        return 0;
    }

    @Override
    public int getOutputMasks() {
        return createMask(new Pin[]{pins[0].getPin(), pins[1].getPin(), pins[2].getPin(), pins[3].getPin(), pins[4].getPin(), pins[5].getPin()});
    }

    @Override
    public int getInitialOutputValues() {
        return getOutputMasks();
    }
}