package controller.actor;

import app.NodeInfoCollector;
import node.Node;
import node.NodePin;
import org.apache.log4j.Logger;
import packet.Packet;

import java.io.IOException;

public class PwmActor extends AbstractActor implements IOnOffActor {
    static Logger log = Logger.getLogger(PwmActor.class.getName());

    int value;

    Indicator[] indicators;
    private int retryCount = 5;


    public PwmActor(String id, NodePin output, int initValue, Indicator... indicators) {
        super(id, output, initValue);

        this.value = initValue;
        this.indicators = indicators;
    }

    public String toString() {
        StringBuilder val = new StringBuilder(String.format("OnOffActor(%s) %s", id, output));
        if (indicators != null) {
            val.append(", indicators: ");
            for (Indicator i : indicators) {
                val.append(i.getPin());
                val.append(", ");
            }
        }
        return val.toString();
    }

    @Override
    public int getValue() {
        return value;
    }

    public synchronized void setIndicators(boolean invert, Object actionData) {
        this.actionData = actionData;
        notifyAll();

        if (indicators != null) {
            for (Indicator i : indicators) {
                int indVal = (i.IsInverted() ^ invert) ? (value ^ 1) & 1 : value;
                setPinValue(i.getPin(), indVal, retryCount);
            }
        }
    }

    @Override
    public synchronized boolean setValue(int val, Object actionData) {
        this.actionData = actionData;
        notifyAll();

        if (setPwmValue(output, val, retryCount)) {
            value = val;
            setIndicators(false, actionData);
            return true;
        }
        return false;
    }

    public boolean isOn() {
        return value != 16;
    }

    private boolean setValue(int val) {
        if (setPwmValue(output, val, 3)) {
            value = val;
            return true;
        }
        return false;
    }

    public boolean increasePwm(int step) {
        int val = (value + step > 16) ? 16 : value + step;
        return setValue(val);
    }


    public boolean decreasePwm(int step) {
        int val = (value - step < 0) ? 0 : value - step;
        return setValue(val);
    }

    @Override
    public boolean switchOn(Object actionData) {
        return setValue(16);
    }

    @Override
    public boolean switchOff(Object actionData) {
        return setValue(0);
    }

    private static boolean setPwmValue(NodePin nodePin, int value, int retryCount) {
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

            } catch (IOException e) {
                log.error(String.format("setPwmValue %s failed.", nodePin.toString()), e);
            }
        }
        return false;
    }
}