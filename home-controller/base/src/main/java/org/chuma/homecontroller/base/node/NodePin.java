package org.chuma.homecontroller.base.node;

public class NodePin {
    String id;
    private final Node node;
    private final Pin pin;

    public NodePin(String id, Node node, Pin pin) {
        this.id = id;
        this.node = node;
        this.pin = pin;
    }

    public String toString() {
        return String.format("%s(node%d.%s)", id, node.getNodeId(), pin);
    }

    public Node getNode() {
        return node;
    }

    public Pin getPin() {
        return pin;
    }

    public String getId() {
        return id;
    }
}