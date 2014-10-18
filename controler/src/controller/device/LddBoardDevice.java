package controller.device;

import node.CpuFrequency;
import node.Node;
import node.NodePin;
import node.Pin;

public class LddBoardDevice extends ConnectedDevice {

    private static final String[] PIN_NAMES = new String[]{"ldd6", "ldd5", "ldd4", "ldd3", "ldd2", "ldd1"};

    public LddBoardDevice(String id, Node node, int connectorPosition) {
        super(id, node, connectorPosition, PIN_NAMES, CpuFrequency.eightMHz);
    }

    public NodePin getLdd1() {
        return pins[5];
    }

    public NodePin getLdd2() {
        return pins[4];
    }

    public NodePin getLdd3() {
        return pins[3];
    }

    public NodePin getLdd4() {
        return pins[2];
    }

    public NodePin getLdd5() {
        return pins[1];
    }

    public NodePin getLdd6() {
        return pins[0];
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