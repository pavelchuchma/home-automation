package controller.actor;

import app.NodeInfoCollector;
import node.Node;
import node.NodePin;
import org.apache.log4j.Logger;
import packet.Packet;

import java.io.IOException;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class PwmActor extends AbstractActor implements IOnOffActor {
    public static final int MAX_PWM_VALUE = 48;
    static Logger log = Logger.getLogger(PwmActor.class.getName());

    int maxPwmValue;


    public PwmActor(String id, NodePin output, double maxLoad, Indicator... indicators) {
        super(id, output, 0, indicators);
        if (maxLoad < 0 || maxLoad > 1) {
            throw new IllegalArgumentException("Invalid maxLoad value: " + maxLoad);
        }

        this.maxPwmValue = (int) (MAX_PWM_VALUE * maxLoad);
    }

    @Override
    public synchronized boolean setValue(int val, Object actionData) {
        validateValue(val);
        val = min(val, maxPwmValue);

        this.actionData = actionData;
        notifyAll();

        if (setPwmValue(output, val, RETRY_COUNT)) {
            value = val;
            setIndicators(true, actionData);
            return true;
        }
        return false;
    }

    private static void validateValue(int val) {
        if (val < 0 || val > MAX_PWM_VALUE) {
            throw new IllegalArgumentException("Invalid PWM value: " + val);
        }
    }

    public boolean isOn() {
        return value != 0;
    }

    public boolean increasePwm(int step) {
        int val = min(value + step, maxPwmValue);
        return setValue(val, null);
    }

    public boolean decreasePwm(int step) {
        int val = max(value - step, 0);
        return setValue(val, null);
    }

    @Override
    public boolean switchOn(Object actionData) {
        return setValue(MAX_PWM_VALUE, actionData);
    }

    @Override
    public boolean switchOff(Object actionData) {
        return setValue(0, actionData);
    }

    private static boolean setPwmValue(NodePin nodePin, int value, int retryCount) {
        validateValue(value);

        NodeInfoCollector nodeInfoCollector = NodeInfoCollector.getInstance();
        Node node = nodeInfoCollector.getNode(nodePin.getNodeId());
        if (node == null) {
            throw new IllegalStateException(String.format("Node #%d not found in repository.", nodePin.getNodeId()));
        }

        for (int i = 0; i < retryCount; i++) {
            try {
                log.debug(String.format("Setting pwm %s to: %d", nodePin, value));
                Packet response = node.setManualPwmValue(nodePin.getPin(), value);
                if (response == null) {
                    throw new IOException("No response.");
                }
                if (response.data == null || response.data.length != 1) {
                    throw new IOException(String.format("Unexpected response length %s", response.toString()));
                }

                if (response.data[0] != 0) {
                    throw new IOException(String.format("Unexpected response code (%d): %s", response.data[0], response.toString()));
                }

                log.info(String.format("PWM of %s set to: %d", nodePin, value));
                return true;

            } catch (Exception e) {
                log.error(String.format("setPwmValue %s failed.", nodePin.toString()), e);
            }
        }
        return false;
    }
}