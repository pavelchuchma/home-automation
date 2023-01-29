package org.chuma.homecontroller.controller.device;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.NodePin;

/**
 * Device with all six pins configured as input. The actually active pins are selected
 * by calling one of {@link #getIn1AndActivate()} methods.
 */
public class GenericInputDevice extends AbstractConnectedDevice {
    private static final String[] PIN_NAMES = {"in1", "in2", "in3", "in4", "in5", "in6"};
    private int eventMask;

    public GenericInputDevice(String id, Node node, int connectorNumber) {
        this(id, node, connectorNumber, PIN_NAMES);
    }

    public GenericInputDevice(String id, Node node, int connectorNumber, String[] names) {
        super(id, node, connectorNumber,false);

        createPins(names);
        finishInit();
    }

    /**
     * Gets input pin and adds returned pin to event mask
     */
    public NodePin getIn1AndActivate() {
        return getPinAndUpdateEventMask(0);
    }

    public NodePin getIn2AndActivate() {
        return getPinAndUpdateEventMask(1);
    }

    public NodePin getIn3AndActivate() {
        return getPinAndUpdateEventMask(2);
    }

    public NodePin getIn4AndActivate() {
        return getPinAndUpdateEventMask(3);
    }

    public NodePin getIn5AndActivate() {
        return getPinAndUpdateEventMask(4);
    }

    public NodePin getIn6AndActivate() {
        return getPinAndUpdateEventMask(5);
    }

    private NodePin getPinAndUpdateEventMask(int index) {
        eventMask |= createMask(pins[index]);
        return pins[index];
    }

    @Override
    public int getEventMask() {
        return eventMask;
    }

    @Override
    public int getOutputMask() {
        return 0;
    }

    @Override
    protected byte getDevicePinOutputMask() {
        return 0;
    }
}