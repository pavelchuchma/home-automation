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
    boolean initializedPwmValue = false;
    int currentPwmValue = 0;
    double currentValue = 0;

    public PwmActor(String name, String label, LddBoardDevice.LddNodePin outputPin, double maxCurrentAmp, ActorListener... actorListeners) {
        super(name, label, outputPin, actorListeners);
        double maxLoad = maxCurrentAmp / outputPin.getMaxLddCurrent();
        Validate.inclusiveBetween(0, 1, maxLoad,
                String.format("Invalid maxLoad value: %s. Bound output %s is not enough for %.2f A",
                        maxLoad, outputPin.getDeviceName(), maxCurrentAmp));

        this.maxPwmValue = (int) (MAX_PWM_VALUE * maxLoad);
    }

    @Override
    public synchronized boolean setValue(double newValue, Object actionData) {
        Validate.inclusiveBetween(0, 1, newValue);

        int newPwmValue = (int) (newValue * maxPwmValue);
        if (newValue > 0 && newPwmValue == 0) {
            // nonzero percent -> set pwm at least to 1
            newPwmValue = 1;
        }

        if ((newPwmValue == currentPwmValue && initializedPwmValue) || setPwmValue(outputPin, newPwmValue)) {
            currentValue = newValue;
            callListenersAndSetActionData(actionData);
            return true;
        }
        return false;
    }

    public boolean isOn() {
        return currentPwmValue > 0;
    }

    public boolean increasePwm(double delta, Object actionData) {
        double val = max(min(getValue() + delta, 1), 0);
        return setValue(val, actionData);
    }

    public boolean decreasePwm(double delta, Object actionData) {
        double val = min(max(getValue() - delta, 0), 1);
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

    private boolean setPwmValue(NodePin nodePin, int pwmValue) {
        Validate.inclusiveBetween(0, maxPwmValue, pwmValue,
                String.format("Invalid PWM value: %d. It must be <= %d", pwmValue, maxPwmValue));
        if (setPinPwmValueImpl(nodePin, pwmValue, AbstractPinActor.RETRY_COUNT)) {
            currentPwmValue = pwmValue;
            initializedPwmValue = true;
            return true;
        }
        return false;
    }

    public static boolean setPinPwmValueImpl(NodePin nodePin, int pwmValue, int retryCount) {
        for (int i = 0; i < retryCount; i++) {
            try {
                log.debug("Setting pwm {} to: {}", nodePin, pwmValue);
                Packet response = nodePin.getNode().setManualPwmValue(nodePin.getPin(), pwmValue);

                if (response == null) {
                    throw new IOException("No response.");
                }
                if (response.data == null || response.data.length != 1) {
                    throw new IOException(String.format("Unexpected response length %s", response));
                }
                if (response.data[0] != 0) {
                    throw new IOException(String.format("Unexpected response code (%d): %s", response.data[0], response));
                }

                log.info("PWM of {} set to: {}", nodePin, pwmValue);
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
        return currentValue;
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