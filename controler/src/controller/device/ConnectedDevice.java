package controller.device;

import node.CpuFrequency;
import node.Node;
import node.NodePin;
import node.Pin;

public abstract class ConnectedDevice {
    NodePin[] pins = new NodePin[6];
    CpuFrequency requiredCpuFrequency;
    final static Pin[][] layout = {
            {Pin.pinA5, Pin.pinA3, Pin.pinA2, Pin.pinA0, Pin.pinB4, Pin.pinB5},
            {Pin.pinC3, Pin.pinC1, Pin.pinC0, Pin.pinA6, Pin.pinC2, Pin.pinA7},
            {Pin.pinB0, Pin.pinB1, Pin.pinC5, Pin.pinC4, Pin.pinC6, Pin.pinC7}
    };


    public ConnectedDevice(String id, Node node, int connectorPosition, String[] names) {
        this(id, node, connectorPosition, names, CpuFrequency.unknown);
    }

    public ConnectedDevice(String id, Node node, int connectorPosition, String[] names, CpuFrequency requiredCpuFrequency) {
        if (names.length != 6) {
            throw new IllegalArgumentException(String.format("Invalid names length: %d", names.length));
        }

        for (int i = 0; i < 6; i++) {
            pins[i] = new NodePin(String.format("%s:%d.%s", id, connectorPosition, names[i]), node.getNodeId(), getPin(connectorPosition, i + 1));
        }

        this.requiredCpuFrequency = requiredCpuFrequency;

        node.addDevice(this);
    }

    /**
     * @param connectorPosition 1-3
     * @param connectorPin      1-6
     * @return
     */
    public static Pin getPin(int connectorPosition, int connectorPin) {
        if (connectorPosition < 1 || connectorPosition > 3) {
            throw new IllegalArgumentException(String.format("Invalid connector position: %d. It must be 1..3", connectorPosition));
        }

        if (connectorPin < 1 || connectorPin > 6) {
            throw new IllegalArgumentException(String.format("Invalid connector pin#: %d. It must be 1..6", connectorPin));
        }

        return layout[connectorPosition - 1][connectorPin - 1];
    }

    private static void checkConnectorPossition(int connectorPosition) {
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

    abstract public int getEventMask();

    abstract public int getOutputMasks();

    abstract public int getInitialOutputValues();
}