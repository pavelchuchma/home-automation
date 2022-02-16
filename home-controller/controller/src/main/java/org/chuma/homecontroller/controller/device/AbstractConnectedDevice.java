package org.chuma.homecontroller.controller.device;

import org.apache.commons.lang3.Validate;

import org.chuma.homecontroller.base.node.CpuFrequency;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.NodePin;
import org.chuma.homecontroller.base.node.Pin;

public abstract class AbstractConnectedDevice implements org.chuma.homecontroller.base.node.ConnectedDevice {
    final static Pin[][] layout = {
            {Pin.pinA5, Pin.pinA3, Pin.pinA2, Pin.pinA0, Pin.pinB4, Pin.pinB5},
            {Pin.pinC3, Pin.pinC1, Pin.pinC0, Pin.pinA6, Pin.pinC2, Pin.pinA7},
            {Pin.pinB0, Pin.pinB1, Pin.pinC5, Pin.pinC4, Pin.pinC6, Pin.pinC7}
    };
    private final String id;
    NodePin[] pins = new NodePin[6];
    CpuFrequency requiredCpuFrequency;
    int connectorNumber;

    public AbstractConnectedDevice(String id, Node node, int connectorNumber, String[] names) {
        this(id, node, connectorNumber, names, CpuFrequency.unknown);
    }

    public AbstractConnectedDevice(String id, Node node, int connectorNumber, String[] names, CpuFrequency requiredCpuFrequency) {
        if (names.length != 6) {
            throw new IllegalArgumentException(String.format("Invalid names length: %d", names.length));
        }

        for (int i = 0; i < 6; i++) {
            pins[i] = new NodePin(String.format("%s:%d.%s", id, connectorNumber, names[i]), node, getPin(connectorNumber, i + 1));
        }

        this.connectorNumber = connectorNumber;
        this.id = id;
        this.requiredCpuFrequency = requiredCpuFrequency;

        node.addDevice(this);
    }

    /**
     * @param connectorNumber 1-3
     * @param connectorPin    1-6
     * @return Pin instance according to input coordinates
     */
    public static Pin getPin(int connectorNumber, int connectorPin) {
        Validate.inclusiveBetween(1,3, connectorNumber, String.format("Invalid connector #%d. It must be 1..3", connectorNumber));
        Validate.inclusiveBetween(1,6, connectorPin, String.format("Invalid connector pin#%d. It must be 1..6", connectorPin));
        return layout[connectorNumber - 1][connectorPin - 1];
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", getClass().getSimpleName(), id);
    }

    @Override
    public int getConnectorNumber() {
        return connectorNumber;
    }

    protected int createMask(Pin[] pins) {
        int result = 0;
        for (Pin pin : pins) {
            result |= 1 << pin.ordinal();
        }
        return result;
    }

    public CpuFrequency getRequiredCpuFrequency() {
        return requiredCpuFrequency;
    }
}