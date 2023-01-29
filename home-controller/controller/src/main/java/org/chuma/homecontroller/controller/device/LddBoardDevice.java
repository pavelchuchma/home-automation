package org.chuma.homecontroller.controller.device;

import org.apache.commons.lang3.Validate;
import static java.lang.Math.round;
import static org.chuma.homecontroller.base.packet.Packet.MAX_PWM_VALUE;

import org.chuma.homecontroller.base.node.CpuFrequency;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.Pin;
import org.chuma.homecontroller.base.node.PwmOutputNodePin;

public class LddBoardDevice extends AbstractConnectedDevice {
    public LddBoardDevice(String id, Node node, int connectorNumber, double ldd1Current, double ldd2Current,
                          double ldd3Current, double ldd4Current, double ldd5Current, double ldd6Current) {
        super(id, node, connectorNumber, true, CpuFrequency.sixteenMHz);

        pins[5] = new LddNodePin(this, "ldd1", node, getPin(connectorNumber, 6), ldd1Current);
        pins[4] = new LddNodePin(this, "ldd2", node, getPin(connectorNumber, 5), ldd2Current);
        pins[3] = new LddNodePin(this, "ldd3", node, getPin(connectorNumber, 4), ldd3Current);
        pins[2] = new LddNodePin(this, "ldd4", node, getPin(connectorNumber, 3), ldd4Current);
        pins[1] = new LddNodePin(this, "ldd5", node, getPin(connectorNumber, 2), ldd5Current);
        pins[0] = new LddNodePin(this, "ldd6", node, getPin(connectorNumber, 1), ldd6Current);

        finishInit();
    }

    public LddNodePin getLdd1() {
        return (LddNodePin)pins[5];
    }

    public LddNodePin getLdd2() {
        return (LddNodePin)pins[4];
    }

    public LddNodePin getLdd3() {
        return (LddNodePin)pins[3];
    }

    public LddNodePin getLdd4() {
        return (LddNodePin)pins[2];
    }

    public LddNodePin getLdd5() {
        return (LddNodePin)pins[1];
    }

    public LddNodePin getLdd6() {
        return (LddNodePin)pins[0];
    }

    @Override
    public int getOutputMask() {
        return createMask(pins);
    }

    @Override
    protected byte getDevicePinOutputMask() {
        return 0b0011_1111;
    }

    public static class LddNodePin extends PwmOutputNodePin {
        double maxLddCurrent;

        private LddNodePin(LddBoardDevice device, String name, Node node, Pin pin, double maxLddCurrent) {
            super(String.format("%s:%d.%s", device.getId(), device.getConnectorNumber(), name), name, node, pin, 0);
            this.maxLddCurrent = maxLddCurrent;
        }

        public double getMaxLddCurrent() {
            return maxLddCurrent;
        }

        public String getDeviceName() {
            return String.format("LDD-%d on %s", round(maxLddCurrent * 1000), this);
        }

        public void setMaxOutputCurrent(double maxLightCurrent) {
            Validate.isTrue(getMaxPwmValue() == 0, "setMaxOutputCurrent() can be called only once");
            double maxLoad = maxLightCurrent / maxLddCurrent;
            Validate.inclusiveBetween(0, 1, maxLoad,
                    String.format("Invalid maxLoad value: %s. Bound LDD output %s is not enough for %.2f A",
                            maxLoad, getDeviceName(), maxLightCurrent));
            setMaxPwmValue((int)(MAX_PWM_VALUE * maxLoad));
        }
    }
}