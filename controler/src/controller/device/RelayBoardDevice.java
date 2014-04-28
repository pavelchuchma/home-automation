package controller.device;

import node.CpuFrequency;
import node.Node;
import node.NodePin;
import node.Pin;

public class RelayBoardDevice extends ConnectedDevice {

    private static final String[] PIN_NAMES = new String[]{"relay3", "relay4", "relay1", "relay5", "relay2", "relay6"};

    public RelayBoardDevice(String id, Node node, int connectorPosition) {
        super(id, node, connectorPosition, PIN_NAMES);
    }

    public RelayBoardDevice(String id, Node node, int connectorPosition, CpuFrequency requiredCpuFrequency) {
        super(id, node, connectorPosition, PIN_NAMES, requiredCpuFrequency);
    }

    public NodePin getRele1() {
        return pins[2];
    }

    public NodePin getRele2() {
        return pins[4];
    }

    public NodePin getRele3() {
        return pins[0];
    }

    public NodePin getRele4() {
        return pins[1];
    }

    public NodePin getRele5() {
        return pins[3];
    }

    public NodePin getRele6() {
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