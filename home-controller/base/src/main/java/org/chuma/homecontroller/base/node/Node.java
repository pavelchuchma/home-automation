package org.chuma.homecontroller.base.node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.packet.IPacketUartIO;
import org.chuma.homecontroller.base.packet.IPacketUartIO.PacketReceivedListener;
import org.chuma.homecontroller.base.packet.Packet;

/**
 * Represents single PIC in system. Each node has unique ID which identifies the PIC (is stored in PIC code).
 * <p>
 * For details on following description see PIC18F2585/2680/4585/4680 manual, chapter 10.0 - I/O ports.
 * <p>
 * PIC provides up to four ports (A..D) with 8 pins. Some pins may have several alternate functions depending
 * on peripheral features. If the feature is enabled, the pin may not be used as general purpose I/O pin.
 * For example pins 2 and 3 of port B are used by Controller Area Network (CAN) module. Since whole "home automation"
 * project uses CAN for communication, these pins are unavailable.
 * <p>
 * Each port has two registers for its operation:
 * <ul>
 * <li>TRIS - data direction register (0 - output, 1 - input)
 * <li>PORT - input or output depending on TRIS value
 * </ul>
 * <p>
 * Devices must be bound to node before initializing the node.
 */
public class Node implements PacketReceivedListener {
    public static final int HEART_BEAT_PERIOD = 60;
    public static final int FAST_RESPONSE_TIMEOUT = 100;
    public static final int SLOW_RESPONSE_TIMEOUT = 300;
    public static final int GET_BUILD_TIME_TIMEOUT = 500;
    private static final Object typeInitializationLock = new Object();
    private static final Logger log = LoggerFactory.getLogger(Node.class.getName());

    private final Queue<Listener> listeners = new ConcurrentLinkedQueue<>();
    private final long[] lastChangeTimes = new long[32];
    private final int nodeId;
    private final String name;
    private final IPacketUartIO packetUartIO;
    private final List<ConnectedDevice> devices = new ArrayList<>(3);

    public Node(int nodeId, IPacketUartIO packetUartIO) {
        this(nodeId, "unknown" + nodeId, packetUartIO);
    }

    public Node(int nodeId, String name, IPacketUartIO packetUartIO) {
        this.nodeId = nodeId;
        this.name = name;
        this.packetUartIO = packetUartIO;
        // Register itself to receive messages from corresponding HW node
        packetUartIO.addSpecificReceivedPacketListener(this, nodeId, -1);
    }

    /**
     * Return string representation of byte value in binary format (e.g. 0b00110011).
     */
    public static String asBinary(int i) {
        String bin = "0000000" + Integer.toBinaryString(i);
        return "0b" + bin.substring(bin.length() - 8);
    }

    private static String registerToString(int address, int value) {
        return Pic.toString(address) + "=" + value + " (" + asBinary(value) + ")";
    }

    /**
     * Register new listener for PIC state changes.
     */
    public void addListener(Listener listener) {
        log.debug("addListener: {}", listener);
        listeners.add(listener);
    }

    /**
     * Get connected devices.
     */
    public List<ConnectedDevice> getDevices() {
        return new ArrayList<>(devices);
    }

    /**
     * Remove all connected devices.
     */
    public void removeDevices() {
        devices.clear();
    }

    /**
     * Add connected device to node. Each connected device must have unique connector number.
     */
    public void addDevice(ConnectedDevice device) {
        for (ConnectedDevice d : devices) {
            if (d.getConnectorNumber() == device.getConnectorNumber()) {
                throw new NodeConfigurationException(String.format("Cannot add device %s on connector position %d because" +
                        " it is already used by %s", device, device.getConnectorNumber(), d), "ConnectorAlreadyUsed");
            }
        }
        if ((device.getEventMask() & device.getOutputMasks()) != 0) {
            throw new NodeConfigurationException(String.format("Event and output masks of device %s are not disjunctive: %s vs. %s",
                    device, intAsBinaryString(device.getEventMask()), intAsBinaryString(device.getOutputMasks())), "DeviceEventOutputClash");
        }

        validatePortMask(device, ConnectedDevice::getEventMask, "event", ConnectedDevice::getEventMask, "event", "ConflictingEventMask");
        validatePortMask(device, ConnectedDevice::getOutputMasks, "output", ConnectedDevice::getOutputMasks, "output", "ConflictingOutputMask");
        validatePortMask(device, ConnectedDevice::getEventMask, "event", ConnectedDevice::getOutputMasks, "output", "DeviceEventMaskConflictsExistingOutputMasks");
        validatePortMask(device, ConnectedDevice::getOutputMasks, "output", ConnectedDevice::getEventMask, "event", "DeviceOutputMaskConflictsExistingEventMasks");

        devices.add(device);
    }

    private void validatePortMask(ConnectedDevice device,
                                  Function<ConnectedDevice, Integer> deviceMaskGetter, String deviceMaskName,
                                  Function<ConnectedDevice, Integer> existingMaskGetter, String existingMaskName,
                                  String errorCode) {
        int existingMask = aggregatePortMask(existingMaskGetter);
        final int deviceMask = deviceMaskGetter.apply(device);
        if ((deviceMask & existingMask) != 0) {
            throw new NodeConfigurationException(String.format("Cannot add device %s on node %s because its %s mask [%s] " +
                            "conflicts with aggregated %s mask [%s] of already registered devices on this node.", device, this,
                    deviceMaskName, intAsBinaryString(deviceMask), existingMaskName, intAsBinaryString(existingMask)), errorCode);
        }
    }

    String intAsBinaryString(int n) {
        return String.join(" ", String.format("%32s", Integer.toBinaryString(n)).replace(' ', '0').split("(?<=\\G.{8})"));
    }


    @Override
    public String toString() {
        return String.format("Node%d[%s]", nodeId, name);
    }

    public int getNodeId() {
        return nodeId;
    }

    public String getName() {
        return name;
    }

    /**
     * Let PIC echo given number of bytes (up to five).
     *
     * @param dataLength how many bytes (max is 5)
     * @return 0 if successful or -1 if response not received within timeout
     */
    public synchronized int echo(int dataLength) throws IOException {
        log.debug("echo ({})", dataLength);
        Packet req = Packet.createMsgEchoRequest(nodeId, dataLength);
        Packet response = packetUartIO.send(req, MessageType.MSG_EchoResponse, SLOW_RESPONSE_TIMEOUT);
        if (response == null) return -1;

        // TODO: Some check that data is correct?
        log.debug("  < {}", response);
        return 0;
    }

    /**
     * Read one byte from PIC (data) memory at given address.
     *
     * @return read byte or -1 if response not received within timeout
     */
    public synchronized int readMemory(int address) throws IOException {
        log.debug("readMemory: {}", Pic.toString(address));
        Packet req = Packet.createMsgReadRamRequest(nodeId, address);
        Packet response = packetUartIO.send(req, MessageType.MSG_ReadRamResponse, SLOW_RESPONSE_TIMEOUT);
        if (response == null) return -1;

        log.debug("  < {}", registerToString(address, response.data[0]));
        return response.data[0];
    }

    /**
     * Write one byte to PIC (data) memory at given address.
     *
     * @return original byte value before write or -1 if response not received within timeout
     */
    @SuppressWarnings("unused")
    public synchronized int writeMemory(int address, int mask, int value) throws IOException {
        log.debug("writeMemory: {}(&{})={}", Pic.toString(address), Integer.toBinaryString(mask), value);
        Packet req = Packet.createMsgWriteRamRequest(nodeId, address, mask, value);
        Packet response = packetUartIO.send(req, MessageType.MSG_WriteRamResponse, SLOW_RESPONSE_TIMEOUT);
        if (response == null) return -1;

        log.debug("  < {}", registerToString(address, response.data[0]));
        return response.data[0];
    }

    /**
     * Get PIC code build time.
     */
    public Date getBuildTime() throws IOException {
        log.debug("getBuildTime");
        Packet response = packetUartIO.send(Packet.createMsgGetBuildTime(nodeId), MessageType.MSG_GetBuildTimeResponse, GET_BUILD_TIME_TIMEOUT);
        log.debug("getBuildTime -> {}", response);
        //noinspection MagicConstant
        return (response != null) ?
                new GregorianCalendar(response.data[0] + 2000, response.data[1] - 1, response.data[2], response.data[3], response.data[4]).getTime()
                : null;
    }

    /**
     * Let PIC echo given bytes (a, b, a + 1, a + 2, a + 3).
     *
     * @return second byte (b) or -1 if response not received within timeout
     */
    public int echo(int a, int b) throws IOException {
        log.debug("echo");
        Packet response = packetUartIO.send(Packet.createMsgEchoRequest(nodeId, a, b), MessageType.MSG_EchoResponse, SLOW_RESPONSE_TIMEOUT);
        log.debug("echo -> {}", response);
        return ((response != null) ? response.data[1] : -1);
    }

    public void setPortValueNoWait(char port, int valueMask, int value) throws IOException {
        log.debug("setPortValueNoWait");
        packetUartIO.send(Packet.createMsgSetPort(nodeId, port, valueMask, value, -1, -1));
        log.debug("setPortValueNoWait: done.");
    }

    /**
     * Set port value. It sets port pins specified by mask to corresponding value.
     *
     * @param port      port A..D
     * @param valueMask 8 bit mask to be applied on {@param value}. Only bits corresponding to 1 in mask will be applied to port value.
     * @param value     8 bit value to be set to desired port (after {@param valueMask} application)
     * @return packet with {@link MessageType#MSG_SetPortResponse} or null if response not received within timeout
     */
    public Packet setPortValue(char port, int valueMask, int value) throws IOException {
        return setPortValue(port, valueMask, value, -1, -1);
    }

    /**
     * Set port and TRIS values.
     *
     * @param port      port A..D
     * @param valueMask 8 bit mask to be applied on {@param value}. Only bits corresponding to 1 in mask will be applied to port value.
     * @param value     8 bit value to be set to desired port (after {@param valueMask} application)
     * @param eventMask enable sending events on value change. Registered {@link Listener} instances are called then
     * @param trisValue set port TRIS values to set up input and output pins
     * @return packet with {@link MessageType#MSG_SetPortResponse} or null if response not received within timeout
     */
    public Packet setPortValue(char port, int valueMask, int value, int eventMask, int trisValue) throws IOException {
        log.debug("setPortValue");
        Packet response = packetUartIO.send(
                Packet.createMsgSetPort(nodeId, port, valueMask, value, eventMask, trisValue),
                MessageType.MSG_SetPortResponse, FAST_RESPONSE_TIMEOUT);
        log.debug("setPortValue: done.");
        return response;
    }

    /**
     * Set value of single pin.
     *
     * @param pin   pin to set
     * @param value pin value, 0 to set to 0, non-zero to set to 1
     * @return packet with {@link MessageType#MSG_SetPortResponse} or null if response not received within timeout
     */
    public Packet setPinValue(Pin pin, int value) throws IOException {
        return setPortValue(pin.getPort(), pin.getBitMask(), (value != 0) ? 0xFF : 0x00);
    }

    /**
     * Dump bytes on given PIC addresses to log.
     *
     * @return true if successful, false if any response not received
     */
    public boolean dumpMemory(int[] addresses) {
        boolean res = true;
        for (int address : addresses) {
            int val = -1;
            try {
                val = readMemory(address);
            } catch (IOException e) {
                log.error("err", e);
            }
            if (val < 0) res = false;
            log.info("#{}: {}", nodeId, registerToString(address, val));
        }
        return res;
    }

    /**
     * Enables native (hardware) PWM on the PIC's CCP1 pin.
     * It is possible to reach high frequency and precision using native PWM, but there is only one CCP port on each PIC.
     * <p>
     * Not used, not tested very well.
     */
    public synchronized boolean enablePwm(int cpuFrequency, int canBaudRatePrescaler, int value) throws IOException {
        log.debug("enablePwm");
        Packet req = Packet.createMsgEnablePwmRequest(nodeId, cpuFrequency, canBaudRatePrescaler, value);
        Packet response = packetUartIO.send(req, MessageType.MSG_EnablePwmResponse, SLOW_RESPONSE_TIMEOUT);
        if (response == null) return false;

        log.debug("  < {}", response);
        return true;
    }

    /**
     * Sets value of native PWM on the PIC's CCP1 pin.
     * <p>
     * Not used, not tested very well.
     */
    synchronized void setPwmValue(int value) throws IOException {
        log.debug("setPwmValue");
        Packet req = Packet.createMsgSetPwmValueRequest(nodeId, value);
        packetUartIO.send(req);
    }

    /**
     * Sets value of manually (software) driven PWM. This PWM implementation has fixed duty cycle of size 48 and can
     * be applied on each output pin.
     *
     * @param pin   Target pin.
     * @param value Value from range <0;48>. 0 is for off, 48 stands for full output
     */
    synchronized public Packet setManualPwmValue(Pin pin, int value) throws IOException {
        log.debug("setPwmValue");
        Packet req = Packet.createMsgMSGSetManualPwmValue(nodeId, pin.getPort(), pin.getPinIndex(), value);
        return packetUartIO.send(req, MessageType.MSG_SetManualPwmValueResponse, FAST_RESPONSE_TIMEOUT);
    }

    /**
     * Send initialization finished packet to PIC.
     */
    public synchronized void setInitializationFinished() throws IOException {
        log.debug("setInitializationFinished");
        Packet req = Packet.createMsgInitializationFinished(nodeId);
        packetUartIO.send(req);
    }

    /**
     * Set heart beat period.
     */
    public synchronized void setHeartBeatPeriod(int seconds) throws IOException {
        log.debug("setHeartBeatPeriod");
        Packet req = Packet.createMsgSetHeartBeatPeriod(nodeId, seconds);
        packetUartIO.send(req);
    }

    /**
     * Set PIC CPU frequency.
     *
     * @return true if successful, false if response not received
     */
    public synchronized boolean setFrequency(CpuFrequency cpuFrequency) throws IOException {
        log.debug("setFrequency");
        if (cpuFrequency == CpuFrequency.unknown)
            throw new NodeConfigurationException("Unsupported frequency value: " + cpuFrequency, "InvalidFrequency");
        Packet req = Packet.createMsgSetFrequency(nodeId, cpuFrequency.getValue());
        Packet response = packetUartIO.send(req, MessageType.MSG_SetFrequencyResponse, FAST_RESPONSE_TIMEOUT);
        if (response == null) return false;

        log.debug("  < {}", response);
        return true;
    }

    @Override
    public void packetReceived(final Packet packet) {
        try {
            packetReceivedImpl(packet);
        } catch (Exception e) {
            log.error("Error in packetReceived()", e);
        }
    }

    private void packetReceivedImpl(Packet packet) throws IOException {
        log.debug("packetReceived: {}", packet);
        if (packet.nodeId != nodeId) throw new RuntimeException("Bad handler " + packet.nodeId + "!=" + nodeId);

        // on pin change
        if (packet.messageType >= MessageType.MSG_OnPortAPinChange && packet.messageType <= MessageType.MSG_OnPortDPinChange) {
            int port = packet.messageType - MessageType.MSG_OnPortAPinChange;
            int eventMask = packet.data[0];
            int eventValue = packet.data[1];
            for (int i = 0; i < 8; i++) {
                int pinMask = 1 << i;
                if ((pinMask & eventMask) != 0) {
                    //event on pin[i]
                    Pin pin = Pin.get(port, i);

                    // compute timeSinceChange (-1 for first case)
                    long now = new Date().getTime();
                    long timeSinceChange = (lastChangeTimes[pin.ordinal()] > 0) ? now - lastChangeTimes[pin.ordinal()] : -1;
                    lastChangeTimes[pin.ordinal()] = now;

                    if ((pinMask & eventValue) != 0) {
                        //button UP
                        log.info("button '{}' UP ({}ms)", pin, timeSinceChange);
                        for (Listener listener : listeners) {
                            listener.onButtonUp(this, pin, (int) timeSinceChange);
                        }
                    } else {
                        //button DOWN
                        log.info("button '{}' DOWN ({}ms)", pin, timeSinceChange);
                        for (Listener listener : listeners) {
                            listener.onButtonDown(this, pin, (int) timeSinceChange);
                        }
                    }
                }
            }
            // on reboot
        } else if (packet.messageType == MessageType.MSG_OnReboot) {
            log.info("#{}: Reboot received", nodeId);
            try {
                for (Listener listener : listeners) {
                    listener.onReboot(this, packet.data[0], packet.data[1]);
                }
                setInitializationFinished();
            } catch (RuntimeException e) {
                log.error("Exception caught from onReboot call of node #" + nodeId, e);
            }
        }
    }

    @Override
    public void notifyRegistered(IPacketUartIO packetUartIO) {

    }

    /**
     * Initialize PIC for connected devices. Asks each device for properties and configures the PIC accordingly.
     */
    public void initialize() {
        log.info("Initialization of node: {} started", this);

        int aggregatedOutputMask = aggregatePortMask(ConnectedDevice::getOutputMasks);
        int aggregatedEventMask = aggregatePortMask(ConnectedDevice::getEventMask);
        int aggregatedInitialOutputValues = aggregatePortMask(ConnectedDevice::getInitialOutputValues);
        CpuFrequency reqFrequency = CpuFrequency.unknown;

        // convert all 4 ports into 32-bit values
        for (ConnectedDevice device : devices) {
            if (reqFrequency == CpuFrequency.unknown) {
                reqFrequency = device.getRequiredCpuFrequency();
            }
        }

        for (int attempt = 0; attempt < 20; attempt++) {
            if (doInitialization(aggregatedOutputMask, aggregatedEventMask, aggregatedInitialOutputValues, reqFrequency)) {
                log.info("Initialization of node: {} succeeded", this);
                return;
            }
        }
        log.error("Initialization of node: {} FAILED", this);
        //todo: reboot device and try it again in case of failure!
    }

    /**
     * Returns aggregated bit masks for all ports (A-D)
     */
    private int aggregatePortMask(Function<ConnectedDevice, Integer> maskGetter) {
        int value = 0;
        for (ConnectedDevice device : devices) {
            value |= maskGetter.apply(device);
        }
        return value;
    }

    private boolean doInitialization(int aggregatedOutputMask, int aggregatedEventMask, int aggregatedInitialOutputValues, CpuFrequency reqFrequency) {
        synchronized (typeInitializationLock) {
            try {
                setHeartBeatPeriod(HEART_BEAT_PERIOD);

                for (int port = 0; port < 4; port++) {
                    int valueMask = (aggregatedOutputMask >> port * 8) & 0xFF;
                    int trisMask = (valueMask ^ 0xFF) & 0xFF;
                    int eventMask = (aggregatedEventMask >> port * 8) & 0xFF;
                    int value = (aggregatedInitialOutputValues >> port * 8) & 0xFF;

                    if (valueMask != 0 || eventMask != 0) {
                        if (port == 1) {
                            // don't modify tris for Can port
                            //TRISB3 = 1; //CAN RX
                            //TRISB2 = 0; //CAN TX
                            trisMask = trisMask & 0xF3 | 0x08; //11110011 | 00001000;
                        }
                        if (setPortValue((char) ('A' + port), valueMask, value, eventMask, trisMask) == null) {
                            //todo: validate response value. Existence of response does not need to be enough
                            log.error("Setting of port {} of node {} failed", (char) ('A' + port), this);
                            return false;
                        }
                    }
                }

                if (reqFrequency != CpuFrequency.unknown) {
                    if (!setFrequency(reqFrequency)) {
                        log.error("Setting frequency of node {} failed", this);
                        return false;
                    }
                }
                return true;
            } catch (IOException e) {
                log.error("Node initialization failed!", e);
                return false;
            }
        }
    }

    /**
     * Reset node. Returns response from PIC.
     */
    public Packet reset() throws IOException {
        log.debug("RESET");
        Packet response = packetUartIO.send(
                Packet.createMsgReset(getNodeId()),
                MessageType.MSG_ResetResponse, FAST_RESPONSE_TIMEOUT);
        log.debug("RESET done.");
        return response;
    }

    /**
     * Reads 4 bytes of PIC program memory on given address.
     */
    @SuppressWarnings("unused")
    public int[] readProgramMemory(int address) throws IOException {
        Packet response = packetUartIO.send(
                Packet.createMsgReadProgramMemory(getNodeId(), address),
                MessageType.MSG_ReadProgramResponse, FAST_RESPONSE_TIMEOUT);
        if (response != null) {
            return Arrays.copyOf(response.data, response.data.length);
        }
        return null;
    }

    /**
     * Listener called when PIC reported state change.
     * <p>
     * The methods are named according to the HW implementation. When button is not preset (up state),
     * there is high voltage on input pin (logical 1). When it gets pressed, the pin goes to logical 0.
     * So button down means pin state goes to 0 and button up means pin goes to 1.
     */
    public interface Listener {
        /**
         * Pin changed to down state (0).
         *
         * @param upTime how long the pin was in up state (1), -1 if first state change
         */
        void onButtonDown(Node node, Pin pin, int upTime);

        /**
         * Pin changed to up state (1).
         *
         * @param downTime how long the pin was in down state (0), -1 if first state change
         */
        void onButtonUp(Node node, Pin pin, int downTime);

        /**
         * PIC rebooting - asks for initialization.
         */
        void onReboot(Node node, int pingCounter, int rconValue) throws IOException;
    }
}