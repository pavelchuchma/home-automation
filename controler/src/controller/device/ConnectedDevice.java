package controller.device;

import node.CpuFrequency;
import node.Node;
import node.NodePin;
import node.Pin;

public abstract class ConnectedDevice {
    NodePin[] pins = new NodePin[6];
    CpuFrequency requiredCpuFrequency;

    public ConnectedDevice(String id, Node node, int connectorPosition, String[] names) {
        this(id, node, connectorPosition, names, CpuFrequency.unknown);
    }

    public ConnectedDevice(String id, Node node, int connectorPosition, String[] names, CpuFrequency requiredCpuFrequency) {
        Pin[][] layout = {
                {Pin.pinA5, Pin.pinA3, Pin.pinA2, Pin.pinA0, Pin.pinB4, Pin.pinB5},
                {Pin.pinC3, Pin.pinC1, Pin.pinC0, Pin.pinA6, Pin.pinC2, Pin.pinA7},
                {Pin.pinB0, Pin.pinB1, Pin.pinC5, Pin.pinC4, Pin.pinC6, Pin.pinC7}
        };
        if (names.length != 6) {
            throw new IllegalArgumentException(String.format("Invalid names length: %d", names.length));
        }

        if (connectorPosition < 1 || connectorPosition > 3) {
            throw new IllegalArgumentException(String.format("Invalid connector position: %d", connectorPosition));
        }

        for (int i = 0; i < 6; i++) {
            pins[i] = new NodePin(String.format("%s:%d.%s", id, connectorPosition, names[i]), node.getNodeId(), layout[connectorPosition-1][i]);
        }

        this.requiredCpuFrequency = requiredCpuFrequency;

        node.addDevice(this);
    }

    protected int createMask(Pin[] pins) {
        int result = 0;
        for (Pin pin : pins) {
            result|= 1 << pin.ordinal();
        }
        return result;
    }

    public CpuFrequency getRequiredCpuFrequency() {
        return requiredCpuFrequency;
    }

    abstract public int getEventMask();
    abstract public int getOutputMasks();
    abstract public int getInitialOutputValues();
}