package org.chuma.homecontroller.controller.device;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.NodePin;
import org.chuma.homecontroller.base.node.Pin;

public class RelayBoardDevice extends AbstractConnectedDevice {

    private static final String[] PIN_NAMES = new String[]{"relay3", "relay4", "relay1", "relay5", "relay2", "relay6"};

    public RelayBoardDevice(String id, Node node, int connectorPosition) {
        super(id, node, connectorPosition, PIN_NAMES);
    }

    public NodePin getRelay1() {
        return pins[2];
    }

    public NodePin getRelay2() {
        return pins[4];
    }

    public NodePin getRelay3() {
        return pins[0];
    }

    public NodePin getRelay4() {
        return pins[1];
    }

    public NodePin getRelay5() {
        return pins[3];
    }

    public NodePin getRelay6() {
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
        return 0;
    }
}