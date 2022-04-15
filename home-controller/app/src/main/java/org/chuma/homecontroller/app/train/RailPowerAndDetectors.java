package org.chuma.homecontroller.app.train;

import org.apache.commons.lang3.Validate;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.NodePin;
import org.chuma.homecontroller.controller.device.AbstractConnectedDevice;

/**
 * Controls power for one rail (direction + voltage (PWM)) and provides up to three train pass sensors. 
 */
public class RailPowerAndDetectors extends AbstractConnectedDevice {
    private int inputs;

    /**
     * Configure with given number of detectors (0-3). Note that pins for disabled detectors are in OUTPUT state! 
     */
    public RailPowerAndDetectors(String id, Node node, int connectorNumber, int passDetectors) {
        super(id, node, connectorNumber, new String[] { "speed", "passA", "dir1", "passB", "dir2", "passC" });
        Validate.inclusiveBetween(0, 3, passDetectors, "Number of detectors");
        inputs = passDetectors;
    }

    public NodePin getPowerPin() {
        return pins[0];
    }

    public NodePin getLeftEnablePin() {
        return pins[2];
    }
    
    public NodePin getRightEnablePin() {
        return pins[4];
    }

    public NodePin getPassDetectorA() {
        return pins[1];
    }

    public NodePin getPassDetectorB() {
        return pins[3];
    }

    public NodePin getPassDetectorC() {
        return pins[5];
    }

    @Override
    public int getInitialOutputValues() {
        return 0;
    }

    @Override
    public int getEventMask() {
        switch (inputs) {
            case 0: return 0;
            case 1: return createMask(pins[1]);
            case 2: return createMask(pins[1], pins[3]);
            case 3: return createMask(pins[1], pins[3], pins[5]);
            default: return 0;
        }
    }

    @Override
    public int getOutputMasks() {
        return createMask(pins[0], pins[2], pins[4]);
    }
}
