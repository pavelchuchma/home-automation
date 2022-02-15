package org.chuma.homecontroller.controller.actor;

import java.io.IOException;

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

    int maxPwmValue;
    int pwmValue = 0;


    public PwmActor(String name, String label, LddBoardDevice.LddNodePin output, double maxLoad, ActorListener... actorListeners) {
        super(name, label, output, 0, actorListeners);
        if (maxLoad < 0 || maxLoad > 1) {
            throw new IllegalArgumentException("Invalid maxLoad value: " + maxLoad);
        }

        this.maxPwmValue = (int) (MAX_PWM_VALUE * maxLoad);
    }

    private static void validatePercentageValue(int val) {
        if (val < 0 || val > 100) {
            throw new IllegalArgumentException("Invalid PWM percentage value: " + val + "%");
        }
    }

    @Override
    public synchronized boolean setValue(int pwmPercent, Object actionData) {
        validatePercentageValue(pwmPercent);
        int newPwmValue = (int) (pwmPercent * .01 * maxPwmValue);
        if (pwmPercent > 0 && newPwmValue == 0) {
            // nonzero percent -> set pwm at least to 1
            newPwmValue = 1;
        }

        if (setPwmValue(outputPin, newPwmValue, RETRY_COUNT)) {
            value = pwmPercent;
            callListenersAndSetActionData(actionData);
            return true;
        }
        return false;
    }

    private void validatePwmValue(int val) {
        if (val < 0 || val > maxPwmValue) {
            throw new IllegalArgumentException("Invalid PWM value: " + val);
        }
    }

    public boolean isOn() {
        return value != 0;
    }

    public boolean increasePwm(int step, Object actionData) {
        int val = min(value + step, 100);
        return setValue(val, actionData);
    }

    public boolean decreasePwm(int step, Object actionData) {
        int val = max(value - step, 0);
        return setValue(val, actionData);
    }

    @Override
    public boolean switchOn(int percent, Object actionData) {
        return setValue(percent, actionData);
    }

    @Override
    public boolean switchOff(Object actionData) {
        return setValue(0, actionData);
    }

    private boolean setPwmValue(NodePin nodePin, int value, int retryCount) {
        validatePwmValue(value);

        for (int i = 0; i < retryCount; i++) {
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

                pwmValue = value;
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

    public int getPwmValue() {
        return pwmValue;
    }

    public int getPwmValuePercent() {
        return (int) (pwmValue * 100d / maxPwmValue);
    }

    public double getOutputCurrent() {
        return (double) pwmValue / MAX_PWM_VALUE * getLddOutput().getMaxLddCurrent();
    }

    public double getMaxOutputCurrent() {
        return (double) maxPwmValue / MAX_PWM_VALUE * getLddOutput().getMaxLddCurrent();
    }

    public LddBoardDevice.LddNodePin getLddOutput() {
        return (LddBoardDevice.LddNodePin) outputPin;
    }
}