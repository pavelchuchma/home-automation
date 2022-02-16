package org.chuma.homecontroller.controller.actor;

import java.io.IOException;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.chuma.homecontroller.base.packet.Packet.MAX_PWM_VALUE;

import org.chuma.homecontroller.base.node.NodePin;
import org.chuma.homecontroller.base.packet.Packet;
import org.chuma.homecontroller.controller.device.LddBoardDevice;

public class PwmActor extends AbstractPinActor implements IOnOffActor {
    static Logger log = LoggerFactory.getLogger(PwmActor.class.getName());

    final int maxPwmValue;
    int currentPwmValue = 0;

    public PwmActor(String name, String label, LddBoardDevice.LddNodePin outputPin, double maxCurrentAmp, ActorListener... actorListeners) {
        super(name, label, outputPin, actorListeners);
        double maxLoad = maxCurrentAmp / outputPin.getMaxLddCurrent();
        Validate.inclusiveBetween(0,1, maxLoad,
                String.format("Invalid maxLoad value: %s. Bound output %s is not enough for %.2f A",
                        maxLoad, outputPin.getDeviceName(), maxCurrentAmp));

        this.maxPwmValue = (int) (MAX_PWM_VALUE * maxLoad);
    }

    @Override
    public synchronized boolean setValue(double val, Object actionData) {
        Validate.inclusiveBetween(0,1, val);

        int newPwmValue = (int) (val * maxPwmValue);
        if (val > 0 && newPwmValue == 0) {
            // nonzero percent -> set pwm at least to 1
            newPwmValue = 1;
        }

        if (setPwmValue(outputPin, newPwmValue)) {
            callListenersAndSetActionData(actionData);
            return true;
        }
        return false;
    }

    public boolean isOn() {
        return currentPwmValue > 0;
    }

    public boolean increasePwm(double step, Object actionData) {
        double val = min(getValue() + step, 1);
        return setValue(val, actionData);
    }

    public boolean decreasePwm(double step, Object actionData) {
        double val = max(getValue() - step, 0);
        return setValue(val, actionData);
    }

    @Override
    public boolean switchOn(double value, Object actionData) {
        return setValue(value, actionData);
    }

    @Override
    public boolean switchOff(Object actionData) {
        return setValue(0, actionData);
    }

    private boolean setPwmValue(NodePin nodePin, int value) {
        Validate.inclusiveBetween(0,maxPwmValue, value,
                String.format("Invalid PWM value: %d. It must be <= %d", value, maxPwmValue));

        for (int i = 0; i < AbstractPinActor.RETRY_COUNT; i++) {
            try {
                log.debug(String.format("Setting pwm %s to: %d", nodePin, value));
                Packet response = nodePin.getNode().setManualPwmValue(nodePin.getPin(), value);

                if (response == null) {
                    throw new IOException("No response.");
                }
                if (response.data == null || response.data.length != 1) {
                    throw new IOException(String.format("Unexpected response length %s", response));
                }
                if (response.data[0] != 0) {
                    throw new IOException(String.format("Unexpected response code (%d): %s", response.data[0], response));
                }

                currentPwmValue = value;
                log.info(String.format("PWM of %s set to: %d", nodePin, value));
                return true;

            } catch (Exception e) {
                log.error(String.format("setPwmValue %s failed.", nodePin.toString()), e);
            }
        }
        return false;
    }

    public int getMaxPwmValue() {
        return maxPwmValue;
    }

    public int getCurrentPwmValue() {
        return currentPwmValue;
    }

    @Override
    public double getValue() {
        return (double) currentPwmValue / MAX_PWM_VALUE;
    }

    public double getOutputCurrent() {
        return (double) currentPwmValue / MAX_PWM_VALUE * getLddOutput().getMaxLddCurrent();
    }

    public double getMaxOutputCurrent() {
        return (double) maxPwmValue / MAX_PWM_VALUE * getLddOutput().getMaxLddCurrent();
    }

    public LddBoardDevice.LddNodePin getLddOutput() {
        return (LddBoardDevice.LddNodePin) outputPin;
    }
}