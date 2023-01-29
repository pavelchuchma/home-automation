package org.chuma.homecontroller.controller.device;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.OutputNodePin;

/**
 * Device consisting of six relays, each on one pin. Output pins are initially set to 0 (off).
 */
public class RelayBoardDevice extends AbstractConnectedDevice {
    private static final String[] PIN_NAMES = new String[]{"relay3", "relay4", "relay1", "relay5", "relay2", "relay6"};

    public RelayBoardDevice(String id, Node node, int connectorNumber) {
        super(id, node, connectorNumber, true);

        createPins(PIN_NAMES);
        finishInit();
    }

    public OutputNodePin getRelay1() {
        return (OutputNodePin)pins[2];
    }

    public OutputNodePin getRelay2() {
        return (OutputNodePin)pins[4];
    }

    public OutputNodePin getRelay3() {
        return (OutputNodePin)pins[0];
    }

    public OutputNodePin getRelay4() {
        return (OutputNodePin)pins[1];
    }

    public OutputNodePin getRelay5() {
        return (OutputNodePin)pins[3];
    }

    public OutputNodePin getRelay6() {
        return (OutputNodePin)pins[5];
    }

    @Override
    protected byte getDevicePinOutputMask() {
        return 0b0011_1111;
    }
}