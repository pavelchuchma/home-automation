package node;

import controller.device.ConnectedDevice;
import org.apache.log4j.Logger;
import packet.IPacketUartIO;
import packet.Packet;
import packet.PacketUartIO;

import java.io.IOException;
import java.util.*;


public class Node implements PacketUartIO.PacketReceivedListener {
    public static final int HEART_BEAT_PERIOD = 60;
    static Logger log = Logger.getLogger(Node.class.getName());
    private static Object initializationLock = new Object();

    public interface Listener {
        void onButtonDown(Node node, Pin pin, int upTime);

        void onButtonUp(Node node, Pin pin, int downTime);

        void onReboot(Node node, int pingCounter, int rconValue) throws IOException, IllegalArgumentException;
    }


    int nodeId;
    String name;
    IPacketUartIO packetUartIO;
    List<ConnectedDevice> devices = new ArrayList<ConnectedDevice>(3);

    protected List<Listener> listeners = new ArrayList<Listener>();
    protected long[] downTimes = new long[32];

    public void addListener(Listener listener) {
        log.debug("addListener: " + listener);
        listeners.add(listener);
    }

    public void addDevice(ConnectedDevice device) {
        for (ConnectedDevice d : devices) {
            if (d.getConnectorPosition() == device.getConnectorPosition()) {
                throw  new IllegalArgumentException(String.format("Cannot add device %s on connector position %d because" +
                        " it is already used by %s", device, device.getConnectorPosition(), d.toString()));
            }
        }
        devices.add(device);
    }



    public Node(int nodeId, IPacketUartIO packetUartIO) {
        this(nodeId, "unknown" + nodeId, packetUartIO);
    }

    public Node(int nodeId, String name, IPacketUartIO packetUartIO) {
        this.nodeId = nodeId;
        this.name = name;
        this.packetUartIO = packetUartIO;
        packetUartIO.addSpecificReceivedPacketListener(this, nodeId, -1);
    }

    public String toString() {
        return String.format("Node%d[%s]", nodeId, name);
    }

    public int getNodeId() {
        return nodeId;
    }

    public String getName() {
        return name;
    }

    private static String asBinary(int i) {
        String bin = "0000000" + Integer.toBinaryString(i);
        return "0b" + bin.substring(bin.length() - 8);
    }

    public static String pinToString(int pin) {
        return Character.toString((char) ('A' + pin / 8)) + pin % 8;
    }

    private static String registerToString(int address, int value) {
        return Pic.toString(address) + "=" + value + " (" + asBinary(value) + ")";
    }

    synchronized int echo(int dataLength) throws IOException {
        log.debug("echo (" + dataLength + ")");
        Packet req = Packet.createMsgEchoRequest(nodeId, dataLength);
        Packet response = packetUartIO.send(req, MessageType.MSG_EchoResponse, 300);
        if (response == null) return -1;

        log.debug("  < " + response.toString());
        return 0;
    }

    synchronized public int readMemory(int address) throws IOException {
        log.debug("readMemory: " + Pic.toString(address));
        Packet req = Packet.createMsgReadRamRequest(nodeId, address);
        Packet response = packetUartIO.send(req, MessageType.MSG_ReadRamResponse, 300);
        if (response == null) return -1;

        log.debug("  < " + registerToString(address, response.data[0]));
        return response.data[0];
    }

    synchronized int writeMemory(int address, int mask, int value) throws IOException {
        log.debug("writeMemory: " + Pic.toString(address) + "(&" + Integer.toBinaryString(mask) + ")=" + value);
        Packet req = Packet.createMsgWriteRamRequest(nodeId, address, mask, value);
        Packet response = packetUartIO.send(req, MessageType.MSG_WriteRamResponse, 300);
        if (response == null) return -1;

        log.debug("  < " + registerToString(address, response.data[0]));
        return response.data[0];
    }

    public Date getBuildTime() throws IOException {
        log.debug("getBuildTime");
        Packet response = packetUartIO.send(Packet.createMsgGetBuildTime(nodeId), MessageType.MSG_GetBuildTimeResponse, 300);
        log.debug("getBuildTime -> " + response);
        return (response != null) ?
                new GregorianCalendar(response.data[0] + 2000, response.data[1] - 1, response.data[2], response.data[3], response.data[4]).getTime()
                : null;
    }

    public char echo(int a, int b) throws IOException {
        log.debug("echo");
        Packet response = packetUartIO.send(Packet.createMsgEchoRequest(nodeId, a, b), MessageType.MSG_EchoResponse, 300);
        log.debug("echo -> " + response);
        return (char) ((response != null) ? response.data[1] : 0);
    }

    void setPortValueNoWait(char port, int valueMask, int value) throws IOException, IllegalArgumentException {
        log.debug("setPortValueNoWait");
        packetUartIO.send(Packet.createMsgSetPort(nodeId, port, valueMask, value, -1, -1));
        log.debug("setPortValueNoWait: done.");

    }

    public Packet setPortValue(char port, int valueMask, int value) throws IOException, IllegalArgumentException {
        return setPortValue(port, valueMask, value, -1, -1);
    }

    public Packet setPortValue(char port, int valueMask, int value, int eventMask, int trisValue) throws IOException, IllegalArgumentException {
        log.debug("setPortValue");
        Packet response = packetUartIO.send(
                Packet.createMsgSetPort(nodeId, port, valueMask, value, eventMask, trisValue),
                MessageType.MSG_SetPortResponse, 100);
        log.debug("setPortValue: done.");
        return response;
    }

    public Packet setPinValue(Pin pin, int value) throws IOException, IllegalArgumentException {
        return setPortValue(pin.getPort(), pin.getBitMask(), (value != 0) ? 0xFF : 0x00);
    }

    public boolean dumpMemory(int[] addresses) {
        boolean res = true;
        for (int i = 0; i < addresses.length; i++) {
            int val = -1;
            try {
                val = readMemory(addresses[i]);
            } catch (IOException e) {
                log.error(e);
            }
            if (val < 0) res = false;
            log.info(String.format("#%d: %s", nodeId, registerToString(addresses[i], val)));
        }
        return res;
    }

    synchronized boolean enablePwm(int cpuFrequency, int canBaudRatePrescaler, int value) throws IOException {
        log.debug("enablePwm");
        Packet req = Packet.createMsgEnablePwmRequest(nodeId, cpuFrequency, canBaudRatePrescaler, value);
        Packet response = packetUartIO.send(req, MessageType.MSG_EnablePwmResponse, 300);
        if (response == null) return false;

        log.debug("  < " + response);
        return true;
    }

    synchronized void setPwmValue(int value) throws IOException {
        log.debug("setPwmValue");
        Packet req = Packet.createMsgSetPwmValueRequest(nodeId, value);
        packetUartIO.send(req);
    }

    synchronized public Packet setManualPwmValue(Pin pin, int value) throws IOException, IllegalArgumentException {
        log.debug("setPwmValue");
        Packet req = Packet.createMsgMSGSetManualPwmValue(nodeId, pin.getPort(), pin.getPinIndex(), value);
        return packetUartIO.send(req, MessageType.MSG_SetManualPwmValueResponse, 100);
    }

    synchronized public void setInitializationFinished() throws IOException {
        log.debug("setInitializationFinished");
        Packet req = Packet.createMsgInitializationFinished(nodeId);
        packetUartIO.send(req);
    }

    synchronized public void setHeartBeatPeriod(int seconds) throws IOException {
        log.debug("setHeartBeatPeriod");
        Packet req = Packet.createMsgSetHeartBeatPeriod(nodeId, seconds);
        packetUartIO.send(req);
    }

    synchronized public boolean setFrequency(CpuFrequency cpuFrequency) throws IOException, IllegalArgumentException {
        log.debug("setFrequency");
        if (cpuFrequency == CpuFrequency.unknown)
            throw new IllegalArgumentException("Unsupported frequency value: " + cpuFrequency);
        Packet req = Packet.createMsgSetFrequency(nodeId, cpuFrequency.getValue());
        Packet response = packetUartIO.send(req, MessageType.MSG_SetFrequencyResponse, 300);
        if (response == null) return false;

        log.debug("  < " + response);
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

    public void packetReceivedImpl(Packet packet) throws IOException, IllegalArgumentException {
        log.debug("packetReceived: " + packet);
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
                    long downTime = (downTimes[pin.ordinal()] > 0) ? new Date().getTime() - downTimes[pin.ordinal()] : -1;
                    if ((pinMask & eventValue) != 0) {
                        //button UP
                        log.info("button '" + pin + "' UP (" + downTime + "ms)");
                        for (Listener listener : listeners) {
                            listener.onButtonUp(this, pin, (int) downTime);
                        }
                    } else {
                        //button DOWN
                        log.info("button '" + pin + "' DOWN (" + downTime + "ms)");
                        for (Listener listener : listeners) {
                            listener.onButtonDown(this, pin, (int) downTime);
                        }
                    }
                    downTimes[pin.ordinal()] = new Date().getTime();
                }
            }
            // on reboot
        } else if (packet.messageType == MessageType.MSG_OnReboot) {
            log.info(String.format("#%d: Reboot received", nodeId));
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

    public List<ConnectedDevice> getDevices() {
        List<ConnectedDevice> out = new ArrayList<ConnectedDevice>();
        out.addAll(devices);
        return out;
    }

    public void removeDevices() {
        devices.clear();
    }

    public void initialize() {
        log.info(String.format("Initialization of node: %s started", toString()));

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
                log.info(String.format("Initialization of node: %s succeeded", toString()));
                return;
            }
        }
        log.error(String.format("Initialization of node: %s FAILED", toString()));
        //todo: reboot device and try it again in case of failure!
    }

    private boolean doInitialization(int commonOutputMask, int commonEventMask, int commonInitialOutputValues, CpuFrequency reqFrequency) {
        synchronized (initializationLock) {
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
                            log.error(String.format("Setting of port %c of node %s failed", (char) ('A' + port), toString()));
                            return false;
                        }
                    }
                }

                if (reqFrequency != CpuFrequency.unknown) {
                    if (!setFrequency(reqFrequency)) {
                        log.error(String.format("Setting frequency of node %s failed", toString()));
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
                MessageType.MSG_ResetResponse, 100);
        log.debug("RESET done.");
        return response;
    }

    public int[] readProgramMemory(int address) throws IOException {
        Packet response = packetUartIO.send(
                Packet.createMsgReadProgramMemory(getNodeId(), address),
                MessageType.MSG_ReadProgramResponse, 100);
        if (response != null) {
            return Arrays.copyOf(response.data, response.data.length);
        }
        return null;
    }
}