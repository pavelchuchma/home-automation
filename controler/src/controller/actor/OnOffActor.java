package controller.actor;

import app.NodeInfoCollector;
import node.Node;
import node.Pin;
import org.apache.log4j.Logger;
import packet.Packet;

import java.io.IOException;

public class OnOffActor extends AbstractActor {
    static Logger log = Logger.getLogger(OnOffActor.class.getName());

    int onValue;

    int value;

    boolean invertIndicatorValue;
    int indicatorNodeId;
    Pin indicatorPin;
    private int retryCount = 5;

    public OnOffActor(String id, int nodeId, Pin pin, int initValue, int onValue) {
        this(id, nodeId, pin, initValue, onValue, false, -1, null);
    }

    public OnOffActor(String id, int nodeId, Pin pin, int initValue, int onValue, boolean invertIndicatorValue, int indicatorNodeId, Pin indicatorPin) {
        super(id, nodeId, pin, initValue);
        this.onValue = onValue;

        this.value = initValue;
        this.invertIndicatorValue = invertIndicatorValue;
        this.indicatorNodeId = indicatorNodeId;
        this.indicatorPin = indicatorPin;
    }

    public String toString() {
        String val = String.format("OnOffActor(%s) node%d.%s", id, nodeId, pin);
        if (indicatorPin != null) {
            val += String.format(", indicator: node%d.%s", indicatorNodeId, indicatorPin);
        }
        return val;
    }

    @Override
    public int getPinOutputMask() {
        return 2 << pin.ordinal();
    }

    public void perform() {
        value = (value ^ 1) & 1;

        if (doAction(nodeId, pin, value, retryCount)) {
            if (indicatorPin != null) {
                int indVal = (invertIndicatorValue) ? (value ^ 1) & 1 : value;
                doAction(indicatorNodeId, indicatorPin, indVal, retryCount);
            }
        }
    }

    private static boolean doAction(int nodeId, Pin pin, int value, int retryCount) {
        NodeInfoCollector nodeInfoCollector = NodeInfoCollector.getInstance();
        Node node = nodeInfoCollector.getNode(nodeId);
        if (node == null) {
            throw new IllegalStateException(String.format("Node #%d not found in repository.", nodeId));
        }

        for (int i = 0; i < retryCount; i++) {
            try {
                log.debug(String.format("Setting pin %s on node #d to: %d", pin, nodeId, value));
                Packet response = node.setPinValue(pin, value);
                if (response == null) {
                    throw new IOException("No response.");
                }
                if (response.data == null || response.data.length != 2) {
                    throw new IOException(String.format("Unexpected response length %s", response.toString()));
                }

                int setVal = ((response.data[1] & pin.getBitMask()) != 0) ? 1 : 0;
                log.info(String.format("Node%d.%s set to: %d", nodeId, pin, setVal));
                return true;

            } catch (IOException e) {
                log.error(String.format("SetPin %s on node #d failed.", pin.toString(), nodeId), e);
            }
        }
        return false;
    }
}