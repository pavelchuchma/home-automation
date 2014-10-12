package controller.actor;

import app.NodeInfoCollector;
import node.Node;
import node.NodePin;
import org.apache.log4j.Logger;
import packet.Packet;

import java.io.IOException;

public abstract class AbstractActor implements Actor {
    static Logger log = Logger.getLogger(AbstractActor.class.getName());

    String id;
    NodePin output;
    int initValue;
    Object actionData;

    public AbstractActor(String id, NodePin output, int initValue) {
        this.id = id;
        this.output = output;
        this.initValue = initValue;
    }

    /**
     * Sets pin to value and waits for response
     * @param nodePin
     * @param value  0 or 1
     * @param retryCount count of retries if no valid response is received
     * @return true if pin was set
     */
    protected boolean setPinValue(NodePin nodePin, int value, int retryCount) {
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

    public String getId() {
        return id;
    }

    @Override
    public Object getLastActionData() {
        return actionData;
    }

    public int getInitValue() {
        return initValue;
    }
}