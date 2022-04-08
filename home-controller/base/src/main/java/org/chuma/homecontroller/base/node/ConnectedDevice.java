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
     * Device id
     */
    String getId();

    /**
     * PIC CPU frequency required by device.
     */
    CpuFrequency getRequiredCpuFrequency();

    /**
     * Event mask - pins for which to receive change notifications. Event and Output masks must be disjunctive.
     * <p>
     * The mask consists of 4x8 bits for ports A-D, where bits 0xFF represent port A and 0xFF000000 represent port D
     */
    int getEventMask();

    /**
     * Output mask - pins which should be in output state. Other pins will become input ones.
     * Usually you will want pins in input state to be present in event mask to receive change
     * notifications.
     * <p>
     * The mask consists of 4x8 bits for ports A-D, where bits 0xFF represent port A and 0xFF000000 represent port D
     */
    int getOutputMasks();

    /**
     * Initial values for outputs.
     */
    int getInitialOutputValues();
}
