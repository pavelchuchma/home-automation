package org.chuma.homecontroller.controller.device;

import static java.lang.Math.round;

import org.chuma.homecontroller.base.node.CpuFrequency;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.NodePin;

public class LddBoardDevice extends AbstractConnectedDevice {
    private static final String[] PIN_NAMES = new String[]{"ldd6", "ldd5", "ldd4", "ldd3", "ldd2", "ldd1"};

    public LddBoardDevice(String id, Node node, int connectorPosition, double ldd1Current, double ldd2Current,
                          double ldd3Current, double ldd4Current, double ldd5Current, double ldd6Current) {
        super(id, node, connectorPosition, PIN_NAMES, CpuFrequency.sixteenMHz);

        pins[5] = new LddNodePin(pins[5], ldd1Current);
        pins[4] = new LddNodePin(pins[4], ldd2Current);
        pins[3] = new LddNodePin(pins[3], ldd3Current);
        pins[2] = new LddNodePin(pins[2], ldd4Current);
        pins[1] = new LddNodePin(pins[1], ldd5Current);
        pins[0] = new LddNodePin(pins[0], ldd6Current);
    }

    public LddNodePin getLdd1() {
        return (LddNodePin) pins[5];
    }

    public LddNodePin getLdd2() {
        return (LddNodePin) pins[4];
    }

    public LddNodePin getLdd3() {
        return (LddNodePin) pins[3];
    }

    public LddNodePin getLdd4() {
        return (LddNodePin) pins[2];
    }

    public LddNodePin getLdd5() {
        return (LddNodePin) pins[1];
    }

    public LddNodePin getLdd6() {
        return (LddNodePin) pins[0];
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
        return 0;
    }

    public static class LddNodePin extends NodePin {
        double maxLddCurrent;

        private LddNodePin(NodePin nodePin, double maxLddCurrent) {
            super(nodePin.getId(), nodePin.getNode(), nodePin.getPin());
            this.maxLddCurrent = maxLddCurrent;
        }

        public double getMaxLddCurrent() {
            return maxLddCurrent;
        }

        public String getDeviceName() {
            return String.format("LDD-%d on %s", round(maxLddCurrent * 1000), this);
        }
    }
}