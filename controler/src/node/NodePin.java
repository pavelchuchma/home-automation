package node;

public class NodePin {
    String id;
    private int nodeId;
    private Pin pin;

    public NodePin(int nodeId, Pin pin) {
        this.nodeId = nodeId;
        this.pin = pin;
    }

    public NodePin(String id, int nodeId, Pin pin) {
        this.id = id;
        this.nodeId = nodeId;
        this.pin = pin;
    }

    public String toString() {
        return String.format("%s(node%d.%s)", id, nodeId, pin);
    }

    public int getNodeId() {
        return nodeId;
    }

    public Pin getPin() {
        return pin;
    }

    public String getId() {
        return id;
    }
}