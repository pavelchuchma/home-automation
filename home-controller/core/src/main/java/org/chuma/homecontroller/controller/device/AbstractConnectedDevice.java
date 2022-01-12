package org.chuma.homecontroller.controller.device;

import org.chuma.homecontroller.nodes.node.CpuFrequency;
import org.chuma.homecontroller.nodes.node.Node;
import org.chuma.homecontroller.nodes.node.NodePin;
import org.chuma.homecontroller.nodes.node.Pin;

public abstract class AbstractConnectedDevice implements org.chuma.homecontroller.nodes.node.ConnectedDevice {
    NodePin[] pins = new NodePin[6];
    CpuFrequency requiredCpuFrequency;
    int connectorNumber;
    private final String id;
    final static Pin[][] layout = {
            {Pin.pinA5, Pin.pinA3, Pin.pinA2, Pin.pinA0, Pin.pinB4, Pin.pinB5},
            {Pin.pinC3, Pin.pinC1, Pin.pinC0, Pin.pinA6, Pin.pinC2, Pin.pinA7},
            {Pin.pinB0, Pin.pinB1, Pin.pinC5, Pin.pinC4, Pin.pinC6, Pin.pinC7}
    };

    @Override
    public String toString() {
        return String.format("%s(%s)", getClass().getSimpleName(), id);
    }

    public AbstractConnectedDevice(String id, Node node, int connectorNumber, String[] names) {
        this(id, node, connectorNumber, names, CpuFrequency.unknown);
    }

    @Override
    public int getConnectorNumber() {
        return connectorNumber;
    }

    public AbstractConnectedDevice(String id, Node node, int connectorNumber, String[] names, CpuFrequency requiredCpuFrequency) {
        if (names.length != 6) {
            throw new IllegalArgumentException(String.format("Invalid names length: %d", names.length));
        }

        for (int i = 0; i < 6; i++) {
            pins[i] = new NodePin(String.format("%s:%d.%s", id, connectorNumber, names[i]), node.getNodeId(), getPin(connectorNumber, i + 1));
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
        if (connectorNumber < 1 || connectorNumber > 3) {
            throw new IllegalArgumentException(String.format("Invalid connector #%d. It must be 1..3", connectorNumber));
        }

        if (connectorPin < 1 || connectorPin > 6) {
            throw new IllegalArgumentException(String.format("Invalid connector pin#%d. It must be 1..6", connectorPin));
        }

        return layout[connectorNumber - 1][connectorPin - 1];
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