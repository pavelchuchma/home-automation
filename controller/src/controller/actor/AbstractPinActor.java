package controller.actor;

import java.io.IOException;

import app.NodeInfoCollector;
import node.Node;
import node.NodePin;
import org.apache.log4j.Logger;
import packet.Packet;

public abstract class AbstractPinActor extends AbstractActor {
    protected static final int RETRY_COUNT = 5;
    static Logger log = Logger.getLogger(AbstractPinActor.class.getName());

    NodePin outputPin;
    int initValue;
    int value;

    public AbstractPinActor(String id, String label, NodePin outputPin, int initValue, ActorListener... actorListeners) {
        super(id, label, actorListeners);
        this.outputPin = outputPin;
        this.initValue = initValue;
        this.value = initValue;
    }

    public static boolean setPinValueImpl(NodePin nodePin, int value, int retryCount) {
        if (value != 0 && value != 1) {
            throw new IllegalArgumentException(String.format("Cannot set value %d to pin %s. Pin value can be 0 or 1 only.", value, nodePin));
        }

        NodeInfoCollector nodeInfoCollector = NodeInfoCollector.getInstance();
        Node node = nodeInfoCollector.getNode(nodePin.getNodeId());
        if (node == null) {
            throw new IllegalStateException(String.format("Node #%d not found in repository.", nodePin.getNodeId()));
        }

        for (int i = 0; i < retryCount; i++) {
            try {
                log.debug(String.format("Setting pin %s to: %d", nodePin, value));
                Packet response = node.setPinValue(nodePin.getPin(), value);
                if (response == null) {
                    throw new IOException("No response.");
                }
                if (response.data == null || response.data.length != 2) {
                    throw new IOException(String.format("Unexpected response length %s", response.toString()));
                }

                // verify value in response
                int setVal = ((response.data[1] & nodePin.getPin().getBitMask()) != 0) ? 1 : 0;
                log.info(String.format("%s set to: %d", nodePin, setVal));
                if (setVal != value) {
                    throw new IOException(String.format("%s was set to %d but response value is %d", nodePin, value, setVal));
                }
                return true;

            } catch (IOException e) {
                log.error(String.format("SetPin %s failed.", nodePin.toString()), e);
            }
        }
        return false;
    }

    @Override
    public int getValue() {
        return value;
    }

    /**
     * Sets pin to value and waits for response
     *
     * @param value      0 or 1
     * @param retryCount count of retries if no valid response is received
     * @return true if pin was set
     */
    protected boolean setPinValue(NodePin nodePin, int value, int retryCount) {
        return setPinValueImpl(nodePin, value, retryCount);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(String.format("%s(%s) %s", getClass().getSimpleName(), id, outputPin));
        appendListeners(sb);
        return sb.toString();
    }
}