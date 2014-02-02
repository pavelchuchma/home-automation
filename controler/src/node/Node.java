package node;

import org.apache.log4j.Logger;
import packet.Packet;
import packet.PacketUartIO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Node implements PacketUartIO.PacketReceivedListener {
    static Logger log = Logger.getLogger(Node.class.getName());

    public interface Listener {
        void onButtonDown(Node node, Pin pin);

        void onButtonUp(Node node, Pin pin, int downTime);

        void onReboot(Node node, int pingCounter, int rconValue) throws IOException, IllegalArgumentException;
    }


    int nodeId;
    PacketUartIO packetUartIO;
    int portCTris;
    int portCEventMask;
    int portCValueMask;
    int portCValue;

    protected List<Listener> listeners = new ArrayList<Listener>();
    protected long[] downTimes = new long[32];

    public void addListener(Listener listener) {
        log.debug("addListener: " + listener);
        listeners.add(listener);
    }


    public Node(int nodeId, PacketUartIO packetUartIO) {
        this.nodeId = nodeId;
        this.packetUartIO = packetUartIO;
        packetUartIO.addSpecificReceivedPacketListener(this, nodeId, -1);
    }

    public Node(int nodeId, PacketUartIO packetUartIO, String portCTris, String portCEventMask, String portCValueMask, String portCValue) {
        this(nodeId, packetUartIO);

        this.portCTris = Integer.parseInt(portCTris, 2);
        this.portCEventMask = Integer.parseInt(portCEventMask, 2);
        this.portCValueMask = Integer.parseInt(portCValueMask, 2);
        this.portCValue = Integer.parseInt(portCValue, 2);
    }

    public int getNodeId() {
        return nodeId;
    }

    public int getPortCTris() {
        return portCTris;
    }

    public int getPortCEventMask() {
        return portCEventMask;
    }

    public int getPortCValueMask() {
        return portCValueMask;
    }

    public int getPortCValue() {
        return portCValue;
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

    synchronized int readMemory(int address) throws IOException {
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
                new Date(response.data[0] + 100, response.data[1] - 1, response.data[2], response.data[3], response.data[4])
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
                MessageType.MSG_SetPortResponse, 300);
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

    synchronized public void setManualPwmValue(char port, int pin, int value) throws IOException, IllegalArgumentException {
        log.debug("setPwmValue");
        Packet req = Packet.createMsgMSGSetManualPwmValue(nodeId, port, pin, value);
        packetUartIO.send(req);
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

    synchronized public boolean setFrequency(int cpuFrequency) throws IOException, IllegalArgumentException {
        log.debug("setFrequency");
        if (cpuFrequency != 1 && cpuFrequency != 2 && cpuFrequency != 8 && cpuFrequency != 16) throw new IllegalArgumentException("Unsupported frequency value");
        Packet req = Packet.createMsgSetFrequency(nodeId, cpuFrequency, cpuFrequency - 1);
        Packet response = packetUartIO.send(req, MessageType.MSG_SetFrequencyResponse, 300);
        if (response == null) return false;

        log.debug("  < " + response);
        return true;
    }

    @Override
    public void packetReceived(final Packet packet) {
        //new Thread(new Runnable() {
        //      public void run() {
        try {
            packetReceivedImpl(packet);
        } catch (IOException e) {
            log.error(e);
        } catch (IllegalArgumentException e) {
            log.error(e);
        }
        //}
        //    }).start();
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
                    if ((pinMask & eventValue) != 0) {
                        //button UP
                        long downTime = new Date().getTime() - downTimes[pin.ordinal()];
                        log.info("button '" + pin + "' UP (" + downTime + "ms)");
                        for (Listener listener : listeners) {
                            listener.onButtonUp(this, pin, (int) downTime);
                        }
                    } else {
                        //button DOWN
                        log.info("button '" + pin + "' DOWN");
                        downTimes[pin.ordinal()] = new Date().getTime();
                        for (Listener listener : listeners) {
                            listener.onButtonDown(this, pin);
                        }
                    }

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
}