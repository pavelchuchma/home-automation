package org.chuma.homecontroller.base.node;

/**
 * Represents connected device pin.
 */
public class NodePin {
    private final String id;
    private final String name;
    private final Node node;
    private final Pin pin;

    /**
     * @param id pin ID, should be globally unique although not required
     * @param name pin name inside its "parent" device
     * @param node node pin is connected to
     * @param pin pin in node
     */
    public NodePin(String id, String name, Node node, Pin pin) {
        this.id = id;
        this.name = name;
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

    public String getName() {
        return name;
    }
}