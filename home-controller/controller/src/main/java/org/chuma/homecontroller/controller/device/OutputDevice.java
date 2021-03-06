package org.chuma.homecontroller.controller.device;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.NodePin;

/**
 * Device with all six pins configured as output. All pins are initially set to 1.
 */
public class OutputDevice extends AbstractConnectedDevice {

    private static final String[] PIN_NAMES = new String[]{"out1", "out2", "out3", "out4", "out5", "out6"};

    public OutputDevice(String id, Node node, int connectorPosition) {
        this(id, node, connectorPosition, PIN_NAMES);
    }

    public OutputDevice(String id, Node node, int connectorPosition, String[] names) {
        super(id, node, connectorPosition, names);
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
        return createMask(pins);
    }

    @Override
    public int getInitialOutputValues() {
        return getOutputMasks();
    }
}