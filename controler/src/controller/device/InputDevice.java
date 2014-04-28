package controller.device;

import node.CpuFrequency;
import node.Node;
import node.NodePin;
import node.Pin;

public class InputDevice extends ConnectedDevice {
    private int eventMask;

    public InputDevice(String id, Node node, int connectorPosition) {
        super(id, node, connectorPosition, new String[]{"in1", "in2", "in3", "in4", "in5", "in6"});
    }

    public InputDevice(String id, Node node, int connectorPosition, CpuFrequency requiredCpuFrequency) {
        super(id, node, connectorPosition, new String[]{"in1", "in2", "in3", "in4", "in5", "in6"}, requiredCpuFrequency);
    }

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
        eventMask |= createMask(new Pin[]{pins[index].getPin()});
        return pins[index];
    }

    @Override
    public int getEventMask() {
        return eventMask;
    }

    @Override
    public int getOutputMasks() {
        return 0;
    }

    @Override
    public int getInitialOutputValues() {
        return getOutputMasks();
    }
}