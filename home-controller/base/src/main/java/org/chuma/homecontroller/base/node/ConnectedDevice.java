package org.chuma.homecontroller.base.node;

/**
 * Represents device connected to single node (PIC).
 */
public interface ConnectedDevice {
    /**
     * ID of connector on PIC board where device is connected.
     * Must be unique within node to which it will be connected.
     */
    int getConnectorNumber();

    /**
     * PIC CPU frequency required by device.
     */
    CpuFrequency getRequiredCpuFrequency();

    /**
     * Event mask - pins which should be in input state (others are unchanged).
     */
    int getEventMask();

    /**
     * Output mask - pins which should be in output state (others are unchanged).
     */
    int getOutputMasks();

    /**
     * Initial values for outputs.
     */
    int getInitialOutputValues();
}
