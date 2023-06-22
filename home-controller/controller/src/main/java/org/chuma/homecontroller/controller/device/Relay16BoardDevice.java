package org.chuma.homecontroller.controller.device;

import java.util.Arrays;

import org.apache.commons.lang3.Validate;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.OutputNodePin;

public class Relay16BoardDevice {
    private final GenericOutputDevice[] portDevices;

    public Relay16BoardDevice(String id, Node node) {
        this(id, node, true, true, true);
    }

    public Relay16BoardDevice(String id, Node node, boolean usePort1, boolean usePort2, boolean usePort3) {
        portDevices = new GenericOutputDevice[]{
                (usePort1) ? new GenericOutputDevice(id + "1", node, 1, generateNames(id, new String[]{"10", "9", "8", "7", "6", "5"}), false) : null,
                (usePort2) ? new GenericOutputDevice(id + "2", node, 2, generateNames(id, new String[]{"16", "15", "14", "13", "12", "11"}), false) : null,
                (usePort3) ? new GenericOutputDevice(id + "3", node, 3, generateNames(id, new String[]{"1", "2", "3", "4", "NC", "NC"}), false) : null
        };
    }

    private static String[] generateNames(String prefix, String[] suffixes) {
        return Arrays.stream(suffixes).map(s -> prefix + "-out" + s).toArray(String[]::new);
    }

    /**
     * @param i Relay index from in range <1;16>
     * Attempt to get a port bound to unused segment throws an exception          
     */
    public OutputNodePin getRelay(int i) {
        Validate.inclusiveBetween(1, 16, i);
        switch (i) {
            case 1:
                return getPortDevice(2).getOut1();
            case 2:
                return getPortDevice(2).getOut2();
            case 3:
                return getPortDevice(2).getOut3();
            case 4:
                return getPortDevice(2).getOut4();
            case 5:
                return getPortDevice(0).getOut6();
            case 6:
                return getPortDevice(0).getOut5();
            case 7:
                return getPortDevice(0).getOut4();
            case 8:
                return getPortDevice(0).getOut3();
            case 9:
                return getPortDevice(0).getOut2();
            case 10:
                return getPortDevice(0).getOut1();
            case 11:
                return getPortDevice(1).getOut6();
            case 12:
                return getPortDevice(1).getOut5();
            case 13:
                return getPortDevice(1).getOut4();
            case 14:
                return getPortDevice(1).getOut3();
            case 15:
                return getPortDevice(1).getOut2();
            case 16:
                return getPortDevice(1).getOut1();
        }
        // should not happen as validated at the beginning of this method
        throw new IllegalArgumentException();
    }

    GenericOutputDevice getPortDevice(int portIndex) {
        Validate.inclusiveBetween(0, 2, portIndex);
        Validate.isTrue(portDevices[portIndex] != null, "Device on port %d is not used.", portIndex);
        return portDevices[portIndex];
    }
}
