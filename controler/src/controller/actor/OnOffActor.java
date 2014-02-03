package controller.actor;

import app.NodeInfoCollector;
import node.Node;
import node.NodePin;
import org.apache.log4j.Logger;
import packet.Packet;

import java.io.IOException;

public class OnOffActor extends AbstractActor {
    static Logger log = Logger.getLogger(OnOffActor.class.getName());

    int onValue;

    int value;

    boolean invertIndicatorValue;
    NodePin indicator;
    private int retryCount = 5;

    public OnOffActor(String id, NodePin output, int initValue, int onValue) {
        this(id, output, initValue, onValue, false, null);
    }

    public OnOffActor(String id, NodePin output, int initValue, int onValue, boolean invertIndicatorValue, NodePin indicator) {
        super(id, output, initValue);
        this.onValue = onValue;

        this.value = initValue;
        this.invertIndicatorValue = invertIndicatorValue;
        this.indicator = indicator;
    }

    public String toString() {
        String val = String.format("OnOffActor(%s) %s", id, output);
        if (indicator != null) {
            val += String.format(", indicator: %s", indicator);
        }
        return val;
    }

    @Override
    public NodePin[] getOutputPins() {
        return (indicator != null) ? new NodePin[]{output, indicator} : new NodePin[]{output};
    }

    public void perform() {
        value = (value ^ 1) & 1;

        if (doAction(output, value, retryCount)) {
            if (indicator != null) {
                int indVal = (invertIndicatorValue) ? (value ^ 1) & 1 : value;
                doAction(indicator, indVal, retryCount);
            }
        }
    }

    private static boolean doAction(NodePin nodePin, int value, int retryCount) {
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

                int setVal = ((response.data[1] & nodePin.getPin().getBitMask()) != 0) ? 1 : 0;
                log.info(String.format("%s set to: %d", nodePin, setVal));
                return true;

            } catch (IOException e) {
                log.error(String.format("SetPin %s failed.", nodePin.toString()), e);
            }
        }
        return false;
    }
}