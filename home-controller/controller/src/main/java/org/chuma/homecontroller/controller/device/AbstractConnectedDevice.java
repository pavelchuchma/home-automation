package org.chuma.homecontroller.controller.device;

import org.apache.commons.lang3.Validate;

import org.chuma.homecontroller.base.node.CpuFrequency;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.NodePin;
import org.chuma.homecontroller.base.node.OutputNodePin;
import org.chuma.homecontroller.base.node.Pin;

/**
 * Base implementation of device connected to node (PIC) on default PCB.
 * <p>
 * This implementation provides up to three devices with six pins connected to single node (PIC).
 * The device pins are as follows:
 *
 * <ul>
 * <li>connector 1: A5, A3, A2, A0, B4, B5
 * <li>connector 2: C3, C1, C0, A6, C2, A7
 * <li>connector 3: B0, B1, C5, C4, C6, C7
 * </ul>
 * <p>
 * Check {@link #layouts} field for exact values, the table above may not be up-to-date.
 * <p>
 * The device pins are a bit strange because of PCB layout and because some pins are reserved
 * for internal use (like CANRX and CANTX on port B). From each PCB with single PIC there are three
 * 6-pin (in fact 8 including Vcc and GND) connectors (1-3) leading to PCBs with "devices". So each
 * device instance corresponds to that single HW device.
 * <p>
 * Each subclass needs to allocate <code>pins</code> and then call <code>finishInit()</code> to
 * finish initialization.
 */
public abstract class AbstractConnectedDevice implements org.chuma.homecontroller.base.node.ConnectedDevice {
    public final static Pin[][] layouts = {
            {Pin.pinA5, Pin.pinA3, Pin.pinA2, Pin.pinA0, Pin.pinB4, Pin.pinB5},
            {Pin.pinC3, Pin.pinC1, Pin.pinC0, Pin.pinA6, Pin.pinC2, Pin.pinA7},
            {Pin.pinB0, Pin.pinB1, Pin.pinC5, Pin.pinC4, Pin.pinC6, Pin.pinC7}
    };
    protected final NodePin[] pins = new NodePin[6];
    private final String id;
    private final Node node;
    private final CpuFrequency requiredCpuFrequency;
    private final int connectorNumber;
    private final boolean highValueMeansOn;

    /**
     * Create new device with unknown CPU frequency automatically adding itself to node.
     *
     * @param id               device ID
     * @param node             node (PIC) to which device is connected
     * @param connectorNumber  on which connector (1-3)
     * @param highValueMeansOn Specifies what output value represents logical ON.
     */
    public AbstractConnectedDevice(String id, Node node, int connectorNumber, boolean highValueMeansOn) {
        this(id, node, connectorNumber, highValueMeansOn, CpuFrequency.unknown);
    }

    /**
     * Create new device automatically adding itself to node.
     *
     * @param id                   device ID
     * @param node                 node (PIC) to which device is connected
     * @param connectorNumber      on which connector (1-3)
     * @param highValueMeansOn     Specifies what output value represents logical ON.
     * @param requiredCpuFrequency PIC CPU frequency required by the device
     */
    public AbstractConnectedDevice(String id, Node node, int connectorNumber, boolean highValueMeansOn, CpuFrequency requiredCpuFrequency) {
        this.connectorNumber = connectorNumber;
        this.id = id;
        this.node = node;
        this.requiredCpuFrequency = requiredCpuFrequency;
        this.highValueMeansOn = highValueMeansOn;
    }

    /**
     * Get Pin instance for given connector and pin.
     *
     * @param connectorNumber 1-3
     * @param connectorPin    1-6
     * @return Pin instance according to input coordinates
     */
    public static Pin getPin(int connectorNumber, int connectorPin) {
        Validate.inclusiveBetween(1, 3, connectorNumber, String.format("Invalid connector #%d. It must be 1..3", connectorNumber));
        Validate.inclusiveBetween(1, 6, connectorPin, String.format("Invalid connector pin#%d. It must be 1..6", connectorPin));
        return layouts[connectorNumber - 1][connectorPin - 1];
    }

    /**
     * Default <code>pins</code> allocation. Can be used by subclasses.
     *
     * @param names names of all six pins (this is purely informative, for debugs etc.)
     */
    protected void createPins(String[] names) {
        int mask = getDevicePinOutputMask();
        for (int i = 0; i < 6; i++) {
            final Pin pin = getPin(connectorNumber, i + 1);
            final String pinId = String.format("%s:%d.%s", id, connectorNumber, names[i]);
            if ((mask & 1) == 1) {
                pins[i] = new OutputNodePin(pinId, names[i], node, pin, highValueMeansOn);
            } else {
                pins[i] = new NodePin(pinId, names[i], node, pin);
            }
            mask >>= 1;
        }
    }

    protected void finishInit() {
        node.addDevice(this);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", getClass().getSimpleName(), getId());
    }

    @Override
    public int getConnectorNumber() {
        return connectorNumber;
    }

    @Override
    public String getId() {
        return id;
    }

    public NodePin[] getPins() {
        return pins;
    }

    /**
     * Create mask for given pins. Specified pins will be set to 1 in mask.
     */
    protected int createMask(NodePin... pins) {
        int result = 0;
        for (NodePin pin : pins) {
            result |= 1 << pin.getPin().ordinal();
        }
        return result;
    }

    protected abstract byte getDevicePinOutputMask();

    @Override
    public int getOutputMask() {
        int outputMask = 0;
        int mask = getDevicePinOutputMask();
        for (int i = 0; i < 6; i++) {
            if ((mask & 1) == 1) {
                outputMask |= createMask(pins[i]);
            }
            mask >>= 1;
        }
        return outputMask;
    }

    @Override
    public int getEventMask() {
        return 0;
    }

    public CpuFrequency getRequiredCpuFrequency() {
        return requiredCpuFrequency;
    }

    @Override
    public int getInitialOutputValues() {
        return (highValueMeansOn) ? 0 : getOutputMask();
    }
}