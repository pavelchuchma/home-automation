package org.chuma.homecontroller.base.node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.packet.IPacketUartIO;
import org.chuma.homecontroller.base.packet.Packet;
import org.chuma.homecontroller.base.packet.PacketUartIO;

public class Node implements PacketUartIO.PacketReceivedListener {
    public static final int HEART_BEAT_PERIOD = 60;
    public static final int SET_PORT_TIMEOUT = 100;
    public static final int GET_BUILD_TIME_TIMEOUT = 500;
    public static final int RESPONSE_TIMEOUT = 300;
    private static final Object typeInitializationLock = new Object();
    private static Logger log = LoggerFactory.getLogger(Node.class.getName());

    private final Queue<Listener> listeners = new ConcurrentLinkedQueue<>();
    private long[] lastChangeTimes = new long[32];
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

    private static String asBinary(int i) {
        String bin = "0000000" + Integer.toBinaryString(i);
        return "0b" + bin.substring(bin.length() - 8);
    }

    private static String registerToString(int address, int value) {
        return Pic.toString(address) + "=" + value + " (" + asBinary(value) + ")";
    }

    public void addListener(Listener listener) {
        log.debug("addListener: {}", listener);
        listeners.add(listener);
    }

    public List<ConnectedDevice> getDevices() {
        return new ArrayList<>(devices);
    }

    public void removeDevices() {
        devices.clear();
    }

    public void addDevice(ConnectedDevice device) {
        for (ConnectedDevice d : devices) {
            if (d.getConnectorNumber() == device.getConnectorNumber()) {
                throw new IllegalArgumentException(String.format("Cannot add device %s on connector position %d because" +
                        " it is already used by %s", device, device.getConnectorNumber(), d));
            }
            // TODO: Check that device masks don't overlap - each device has distinct set of pins
        }
        devices.add(device);
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

    public synchronized int echo(int dataLength) throws IOException {
        log.debug("echo ({})", dataLength);
        Packet req = Packet.createMsgEchoRequest(nodeId, dataLength);
        Packet response = packetUartIO.send(req, MessageType.MSG_EchoResponse, RESPONSE_TIMEOUT);
        if (response == null) return -1;

        // TODO: Some check that data is correct?
        log.debug("  < {}", response);
        return 0;
    }

    public synchronized int readMemory(int address) throws IOException {
        log.debug("readMemory: {}", Pic.toString(address));
        Packet req = Packet.createMsgReadRamRequest(nodeId, address);
        Packet response = packetUartIO.send(req, MessageType.MSG_ReadRamResponse, RESPONSE_TIMEOUT);
        if (response == null) return -1;

        log.debug("  < {}", registerToString(address, response.data[0]));
        return response.data[0];
    }

    public synchronized int writeMemory(int address, int mask, int value) throws IOException {
        log.debug("writeMemory: {}(&{})={}", Pic.toString(address), Integer.toBinaryString(mask), value);
        Packet req = Packet.createMsgWriteRamRequest(nodeId, address, mask, value);
        Packet response = packetUartIO.send(req, MessageType.MSG_WriteRamResponse, RESPONSE_TIMEOUT);
        if (response == null) return -1;

        log.debug("  < {}", registerToString(address, response.data[0]));
        return response.data[0];
    }

    public Date getBuildTime() throws IOException {
        log.debug("getBuildTime");
        Packet response = packetUartIO.send(Packet.createMsgGetBuildTime(nodeId), MessageType.MSG_GetBuildTimeResponse, GET_BUILD_TIME_TIMEOUT);
        log.debug("getBuildTime -> {}", response);
        return (response != null) ?
                new GregorianCalendar(response.data[0] + 2000, response.data[1] - 1, response.data[2], response.data[3], response.data[4]).getTime()
                : null;
    }

    public int echo(int a, int b) throws IOException {
        log.debug("echo");
        Packet response = packetUartIO.send(Packet.createMsgEchoRequest(nodeId, a, b), MessageType.MSG_EchoResponse, RESPONSE_TIMEOUT);
        log.debug("echo -> {}", response);
        return ((response != null) ? response.data[1] : -1);
    }

    void setPortValueNoWait(char port, int valueMask, int value) throws IOException {
        log.debug("setPortValueNoWait");
        packetUartIO.send(Packet.createMsgSetPort(nodeId, port, valueMask, value, -1, -1));
        log.debug("setPortValueNoWait: done.");

    }

    public Packet setPortValue(char port, int valueMask, int value) throws IOException {
        return setPortValue(port, valueMask, value, -1, -1);
    }

    public Packet setPortValue(char port, int valueMask, int value, int eventMask, int trisValue) throws IOException {
        log.debug("setPortValue");
        Packet response = packetUartIO.send(
                Packet.createMsgSetPort(nodeId, port, valueMask, value, eventMask, trisValue),
                MessageType.MSG_SetPortResponse, SET_PORT_TIMEOUT);
        log.debug("setPortValue: done.");
        return response;
    }

    public Packet setPinValue(Pin pin, int value) throws IOException {
        return setPortValue(pin.getPort(), pin.getBitMask(), (value != 0) ? 0xFF : 0x00);
    }

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

    synchronized boolean enablePwm(int cpuFrequency, int canBaudRatePrescaler, int value) throws IOException {
        log.debug("enablePwm");
        Packet req = Packet.createMsgEnablePwmRequest(nodeId, cpuFrequency, canBaudRatePrescaler, value);
        Packet response = packetUartIO.send(req, MessageType.MSG_EnablePwmResponse, RESPONSE_TIMEOUT);
        if (response == null) return false;

        log.debug("  < {}", response);
        return true;
    }

    synchronized void setPwmValue(int value) throws IOException {
        log.debug("setPwmValue");
        Packet req = Packet.createMsgSetPwmValueRequest(nodeId, value);
        packetUartIO.send(req);
    }

    synchronized public Packet setManualPwmValue(Pin pin, int value) throws IOException {
        log.debug("setPwmValue");
        Packet req = Packet.createMsgMSGSetManualPwmValue(nodeId, pin.getPort(), pin.getPinIndex(), value);
        return packetUartIO.send(req, MessageType.MSG_SetManualPwmValueResponse, SET_PORT_TIMEOUT);
    }

    public synchronized void setInitializationFinished() throws IOException {
        log.debug("setInitializationFinished");
        Packet req = Packet.createMsgInitializationFinished(nodeId);
        packetUartIO.send(req);
    }

    public synchronized void setHeartBeatPeriod(int seconds) throws IOException {
        log.debug("setHeartBeatPeriod");
        Packet req = Packet.createMsgSetHeartBeatPeriod(nodeId, seconds);
        packetUartIO.send(req);
    }

    public synchronized boolean setFrequency(CpuFrequency cpuFrequency) throws IOException {
        log.debug("setFrequency");
        if (cpuFrequency == CpuFrequency.unknown)
            throw new IllegalArgumentException("Unsupported frequency value: " + cpuFrequency);
        Packet req = Packet.createMsgSetFrequency(nodeId, cpuFrequency.getValue());
        Packet response = packetUartIO.send(req, MessageType.MSG_SetFrequencyResponse, SET_PORT_TIMEOUT);
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

                    // compute downTime (-1 for first case)
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
    public void notifyRegistered(PacketUartIO packetUartIO) {

    }

    public void initialize() {
        log.info("Initialization of node: {} started", this);

        int commonOutputMask = 0;
        int commonEventMask = 0;
        int commonInitialOutputValues = 0;
        CpuFrequency reqFrequency = CpuFrequency.unknown;

        // convert all 4 ports into 32-bit values
        for (ConnectedDevice device : devices) {
            commonOutputMask |= device.getOutputMasks();
            commonEventMask |= device.getEventMask();
            commonInitialOutputValues |= device.getInitialOutputValues();

            if (reqFrequency == CpuFrequency.unknown) {
                reqFrequency = device.getRequiredCpuFrequency();
            }
        }

        for (int attempt = 0; attempt < 20; attempt++) {
            if (doInitialization(commonOutputMask, commonEventMask, commonInitialOutputValues, reqFrequency)) {
                log.info("Initialization of node: {} succeeded", this);
                return;
            }
        }
        log.error("Initialization of node: {} FAILED", this);
        //todo: reboot device and try it again in case of failure!
    }

    private boolean doInitialization(int commonOutputMask, int commonEventMask, int commonInitialOutputValues, CpuFrequency reqFrequency) {
        synchronized (typeInitializationLock) {
            try {
                setHeartBeatPeriod(HEART_BEAT_PERIOD);

                for (int port = 0; port < 4; port++) {
                    int valueMask = (commonOutputMask >> port * 8) & 0xFF;
                    int trisMask = (valueMask ^ 0xFF) & 0xFF;
                    int eventMask = (commonEventMask >> port * 8) & 0xFF;
                    int value = (commonInitialOutputValues >> port * 8) & 0xFF;

                    if (valueMask != 0 || eventMask != 0) {
                        if (port == 1) {
                            // don't modify tris for Can port
                            //TRISB3 = 1; //CAN RX
                            //TRISB2 = 0; //CAN TX
                            trisMask = trisMask & 0xF3 | 0x08; //11110011 | 00001000;
                        }
                        if (setPortValue((char) ('A' + port), valueMask, value, eventMask, trisMask) == null) {
                            //todo: validate response value. Existence of response need not be enough
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

    public Packet reset() throws IOException {
        log.debug("RESET");
        Packet response = packetUartIO.send(
                Packet.createMsgReset(getNodeId()),
                MessageType.MSG_ResetResponse, SET_PORT_TIMEOUT);
        log.debug("RESET done.");
        return response;
    }

    public int[] readProgramMemory(int address) throws IOException {
        Packet response = packetUartIO.send(
                Packet.createMsgReadProgramMemory(getNodeId(), address),
                MessageType.MSG_ReadProgramResponse, SET_PORT_TIMEOUT);
        if (response != null) {
            return Arrays.copyOf(response.data, response.data.length);
        }
        return null;
    }

    public interface Listener {
        void onButtonDown(Node node, Pin pin, int upTime);

        void onButtonUp(Node node, Pin pin, int downTime);

        void onReboot(Node node, int pingCounter, int rconValue) throws IOException;
    }
}