package org.chuma.homecontroller.controller.device;

import org.chuma.homecontroller.base.node.CpuFrequency;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.OutputNodePin;

/**
 * Device with all six pins configured as output. All pins are initially set to logical OFF.
 */
public class GenericOutputDevice extends AbstractConnectedDevice {
    private static final String[] PIN_NAMES = new String[]{"out1", "out2", "out3", "out4", "out5", "out6"};

    public GenericOutputDevice(String id, Node node, int connectorNumber, boolean highValueMeansOn) {
        this(id, node, connectorNumber, PIN_NAMES, highValueMeansOn);
    }

    public GenericOutputDevice(String id, Node node, int connectorNumber, String[] names, boolean highValueMeansOn) {
        super(id, node, connectorNumber, highValueMeansOn);

        createPins(names);
        finishInit();
    }

    /**
     * @param highValueMeansOnMask bit N set means that value 1 represents logical ON on outN+1
     */
    public GenericOutputDevice(String id, Node node, int connectorNumber, String[] names, int highValueMeansOnMask) {
        super(id, node, connectorNumber, highValueMeansOnMask, CpuFrequency.unknown);

        createPins(names);
        finishInit();
    }

    @Override
    protected byte getDevicePinOutputMask() {
        return 0b0011_1111;
    }

    public OutputNodePin getOut1() {
        return (OutputNodePin)pins[0];
    }

    public OutputNodePin getOut2() {
        return (OutputNodePin)pins[1];
    }

    public OutputNodePin getOut3() {
        return (OutputNodePin)pins[2];
    }

    public OutputNodePin getOut4() {
        return (OutputNodePin)pins[3];
    }

    public OutputNodePin getOut5() {
        return (OutputNodePin)pins[4];
    }

    public OutputNodePin getOut6() {
        return (OutputNodePin)pins[5];
    }
}