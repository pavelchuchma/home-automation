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
     * Event mask - pins for which to receive change notifications. Remember to clear such pins
     * in output mask since otherwise the pin won't be in input state.
     */
    int getEventMask();

    /**
     * Output mask - pins which should be in output state. Other pins will become input ones.
     * Usually you will want pins in input state to be present in event mask to receive change
     * notifications.
     */
    int getOutputMasks();

    /**
     * Initial values for outputs.
     */
    int getInitialOutputValues();
}
