package org.chuma.homecontroller.controller.actor;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.node.NodePin;
import org.chuma.homecontroller.base.node.OutputNodePin;
import org.chuma.homecontroller.base.packet.Packet;

public abstract class AbstractPinActor extends AbstractActor {
    protected static final int RETRY_COUNT = 5;
    static Logger log = LoggerFactory.getLogger(AbstractPinActor.class.getName());

    OutputNodePin outputPin;

    public AbstractPinActor(String id, String label, OutputNodePin outputPin, ActorListener... actorListeners) {
        super(id, label, actorListeners);
        this.outputPin = outputPin;
    }

    public static boolean setPinValueImpl(OutputNodePin nodePin, boolean value, int retryCount) {
        int newValue = (value == nodePin.isHighValueMeansOn()) ? 1 : 0;
        for (int i = 0; i < retryCount; i++) {
            try {
                log.debug("Setting pin {} to: {}", nodePin, newValue);
                Packet response = nodePin.getNode().setPinValue(nodePin.getPin(), newValue);
                if (response == null) {
                    throw new IOException("No response.");
                }
                if (response.data == null || response.data.length != 2) {
                    throw new IOException(String.format("Unexpected response length %s", response));
                }

                // verify value in response
                int setVal = ((response.data[1] & nodePin.getPin().getBitMask()) != 0) ? 1 : 0;
                log.info("{} set to: {}", nodePin, setVal);
                if (setVal != newValue) {
                    throw new IOException(String.format("%s was set to %d but response value is %d", nodePin, newValue, setVal));
                }
                return true;

            } catch (IOException e) {
                log.error(String.format("SetPin %s failed.", nodePin), e);
            }
        }
        return false;
    }

    /**
     * Sets pin to value and waits for response
     *
     * @param value      target value, physical value depends on nodePin.highValueMeansOn
     * @param retryCount count of retries if no valid response is received
     * @return true if pin was set
     */
    protected boolean setPinValue(OutputNodePin nodePin, boolean value, int retryCount) {
        return setPinValueImpl(nodePin, value, retryCount);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(String.format("%s(%s) %s", getClass().getSimpleName(), id, outputPin));
        appendListeners(sb);
        return sb.toString();
    }
}