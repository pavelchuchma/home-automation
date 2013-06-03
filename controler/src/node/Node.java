package node;

import com.sun.javaws.exceptions.InvalidArgumentException;
import org.apache.log4j.Logger;
import packet.Packet;
import packet.PacketUartIO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Node implements PacketUartIO.PacketReceivedListener {
    static Logger log = Logger.getLogger(Node.class.getName());

    public static final int pinA0 = 0;
    public static final int pinA1 = 1;
    public static final int pinA2 = 2;
    public static final int pinA3 = 3;
    public static final int pinA4 = 4;
    public static final int pinA5 = 5;
    public static final int pinA6 = 6;
    public static final int pinA7 = 7;
    public static final int pinB0 = 8;
    public static final int pinB1 = 9;
    public static final int pinB2 = 10;
    public static final int pinB3 = 11;
    public static final int pinB4 = 12;
    public static final int pinB5 = 13;
    public static final int pinB6 = 14;
    public static final int pinB7 = 15;
    public static final int pinC0 = 16;
    public static final int pinC1 = 17;
    public static final int pinC2 = 18;
    public static final int pinC3 = 19;
    public static final int pinC4 = 20;
    public static final int pinC5 = 21;
    public static final int pinC6 = 22;
    public static final int pinC7 = 23;
    public static final int pinD0 = 24;
    public static final int pinD1 = 25;
    public static final int pinD2 = 26;
    public static final int pinD3 = 27;
    public static final int pinD4 = 28;
    public static final int pinD5 = 29;
    public static final int pinD6 = 30;
    public static final int pinD7 = 31;

    interface Listener {
        void onButtonDown(Node node, int pin);

        void onButtonUp(Node node, int pin, int downTime);

        void onReboot(Node node, int pingCounter, int rconValue) throws IOException, InvalidArgumentException;
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

    Date getBuildTime() throws IOException {
        log.debug("getBuildTime");
        Packet response = packetUartIO.send(Packet.createMsgGetBuildTime(nodeId), MessageType.MSG_GetBuildTimeResponse, 300);
        log.debug("getBuildTime -> " + response);
        return (response != null) ?
                new Date(response.data[0] + 100, response.data[1] - 1, response.data[2], response.data[3], response.data[4])
                : null;
    }

    void setPortValueNoWait(char port, int valueMask, int value) throws IOException, InvalidArgumentException {
        log.debug("setPortValueNoWait");
        packetUartIO.send(Packet.createMsgSetPort(nodeId, port, valueMask, value, -1, -1));
        log.debug("setPortValueNoWait: done.");

    }

    Packet setPortValue(char port, int valueMask, int value) throws IOException, InvalidArgumentException {
        return setPortValue(port, valueMask, value, -1, -1);
    }

    Packet setPortValue(char port, int valueMask, int value, int eventMask, int trisValue) throws IOException, InvalidArgumentException {
        log.debug("setPortValue");
        Packet response = packetUartIO.send(
                Packet.createMsgSetPort(nodeId, port, valueMask, value, eventMask, trisValue),
                MessageType.MSG_SetPortResponse, 300);
        log.debug("setPortValue: done.");
        return response;
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

    synchronized void setInitializationFinished() throws IOException {
        log.debug("setInitializationFinished");
        Packet req = Packet.createMsgInitializationFinished(nodeId);
        packetUartIO.send(req);
    }

    synchronized void setHeartBeatPeriod(int seconds) throws IOException {
        log.debug("setHeartBeatPeriod");
        Packet req = Packet.createMsgSetHeartBeatPeriod(nodeId, seconds);
        packetUartIO.send(req);
    }

    @Override
    public void packetReceived(final Packet packet) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    packetReceivedImpl(packet);
                } catch (IOException e) {
                    log.error(e);
                } catch (InvalidArgumentException e) {
                    log.error(e);
                }
            }
        }).start();
    }

    public void packetReceivedImpl(Packet packet) throws IOException, InvalidArgumentException {
        log.debug("packetReceived: " + packet);
        if (packet.nodeId != nodeId) throw new RuntimeException("Bad handler " + packet.nodeId + "!=" + nodeId);

        if (packet.messageType >= MessageType.MSG_OnPortAPinChange && packet.messageType <= MessageType.MSG_OnPortDPinChange) {
            int port = packet.messageType - MessageType.MSG_OnPortAPinChange;
            int eventMask = packet.data[0];
            int eventValue = packet.data[1];
            for (int i = 0; i < 8; i++) {
                int pinMask = 1 << i;
                if ((pinMask & eventMask) != 0) {
                    //event on pin[i]
                    int pin = port * 8 + i;
                    if ((pinMask & eventValue) != 0) {
                        //button UP
                        long downTime = new Date().getTime() - downTimes[pin];
                        log.info("button '" + pinToString(pin) + "' UP (" + downTime + "ms)");
                        for (int l = 0; l < listeners.size(); l++) {
                            listeners.get(l).onButtonUp(this, pin, (int) downTime);
                        }
                    } else {
                        //button DOWN
                        log.info("button '" + pinToString(pin) + "' DOWN");
                        downTimes[pin] = new Date().getTime();
                        for (int l = 0; l < listeners.size(); l++) {
                            listeners.get(l).onButtonDown(this, pin);
                        }
                    }

                }
            }

        } else if (packet.messageType == MessageType.MSG_OnReboot) {
            log.info(String.format("#%d: Reboot received", nodeId));
            for (int l = 0; l < listeners.size(); l++) {
                listeners.get(l).onReboot(this, packet.data[0], packet.data[1]);
            }
            setInitializationFinished();
        }
    }

    @Override
    public void notifyRegistered(PacketUartIO packetUartIO) {

    }
}