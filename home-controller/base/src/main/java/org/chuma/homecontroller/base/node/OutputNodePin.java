package org.chuma.homecontroller.base.node;

public class OutputNodePin extends NodePin {
    private final boolean highValueMeansOn;

    /**
     * @param id               pin ID, should be globally unique although not required
     * @param name             pin name inside its "parent" device
     * @param node             node pin is connected to
     * @param pin              pin in node
     * @param highValueMeansOn true means that switching pin ON means settings it to 1
     */
    public OutputNodePin(String id, String name, Node node, Pin pin, boolean highValueMeansOn) {
        super(id, name, node, pin);
        this.highValueMeansOn = highValueMeansOn;
    }

    /**
     * @return true if ON state is represented by value 1 and OFF state by 0. Returns false if it is opposite.
     */
    public boolean isHighValueMeansOn() {
        return highValueMeansOn;
    }
}
