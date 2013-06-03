package packet;

import gnu.io.*;
import node.MessageType;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class PacketUartIO implements SerialPortEventListener {
    private class ResponseWrapper implements PacketReceivedListener {
        private Packet packet;

        protected ResponseWrapper(int nodeId, int responseType) {
            addSpecificReceivedPacketListener(this, nodeId, responseType);
        }

        @Override
        synchronized public void packetReceived(Packet packet) {
            this.packet = packet;
            notify();
        }

        @Override
        public void notifyRegistered(PacketUartIO packetUartIO) {

        }

        synchronized public Packet waitForResponse(int timeout) {
            try {
                // wait for packet, if not received yet
                if (packet == null) wait(timeout);
            } catch (InterruptedException e) {
                log.error("waitForResponse terminated", e);
            }
            return packet;
        }
    }


    static Logger log = Logger.getLogger(PacketUartIO.class.getName());
    static Logger msgLog = Logger.getLogger(PacketUartIO.class.getName() + ".msg");

    // Listener interface
    public interface PacketReceivedListener {
        void packetReceived(Packet packet);

        void notifyRegistered(PacketUartIO packetUartIO);
    }

    protected List<PacketReceivedListener> listeners = new ArrayList<PacketReceivedListener>();
    protected Map<String, PacketReceivedListener> specificListeners = new HashMap<String, PacketReceivedListener>();

    SerialPort serialPort;
    InputStream inputStream;
    PacketSerializer packetSerializer = new PacketSerializer();

    public PacketUartIO(String portName, int baudRate) throws PacketUartIOException {
        log.debug("Creating '" + portName + "' @" + baudRate + " bauds...");
        Enumeration portList = CommPortIdentifier.getPortIdentifiers();

        while (portList.hasMoreElements()) {
            CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                if (portId.getName().equals(portName)) {
                    // if (portId.getName().equals("/dev/term/a")) {
                    try {
                        serialPort = (SerialPort) portId.open("SimpleReadApp", 2000);
                        inputStream = serialPort.getInputStream();
                        serialPort.addEventListener(this);
                        serialPort.notifyOnDataAvailable(true);
                        serialPort.setSerialPortParams(baudRate,
                                SerialPort.DATABITS_8,
                                SerialPort.STOPBITS_1,
                                SerialPort.PARITY_NONE);
                        log.debug("  serial port listener started");
                        return;
                    } catch (Exception e) {
                        log.error("Cannot open serial port", e);
                        close();
                        throw new PacketUartIOException(e);
                    }
                }
            }
        }
        throw new PacketUartIOException(new NoSuchPortException());
    }

    private String serialPortEventToString(SerialPortEvent e) {
        switch (e.getEventType()) {
            case SerialPortEvent.BI:
                return "BI";
            case SerialPortEvent.OE:
                return "OE";
            case SerialPortEvent.FE:
                return "FE";
            case SerialPortEvent.PE:
                return "PE";
            case SerialPortEvent.CD:
                return "CD";
            case SerialPortEvent.CTS:
                return "CTS";
            case SerialPortEvent.DSR:
                return "DSR";
            case SerialPortEvent.RI:
                return "RI";
            case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                return "OUTPUT_BUFFER_EMPTY";
            case SerialPortEvent.DATA_AVAILABLE:
                return "DATA_AVAILABLE";
        }
        return "unknown(" + e.getEventType() + ")";
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        log.trace("serialEvent: " + serialPortEventToString(event));
        switch (event.getEventType()) {
            case SerialPortEvent.BI:
            case SerialPortEvent.OE:
            case SerialPortEvent.FE:
            case SerialPortEvent.PE:
            case SerialPortEvent.CD:
            case SerialPortEvent.CTS:
            case SerialPortEvent.DSR:
            case SerialPortEvent.RI:
            case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                break;
            case SerialPortEvent.DATA_AVAILABLE:
                try {
                    Packet receivedPacket = packetSerializer.readPacket(inputStream);
                    if (receivedPacket != null) {
                        processPacket(receivedPacket);
                    }
                } catch (IOException e) {
                    msgLog.error("receiveError", e);
                    log.error(e);
                    e.printStackTrace();
                }
                break;
        }
    }

    void processPacket(Packet packet) {
        msgLog.debug(" > " + packet);

        PacketReceivedListener specificListener = specificListeners.get(createSpecificListenerKey(packet.nodeId, packet.messageType));
        if (specificListener != null) {
            log.debug("Calling processPacket.specificListener (" + specificListener + ") for: " + packet);
            specificListener.packetReceived(packet);
        }

        specificListener = specificListeners.get(createSpecificListenerKey(packet.nodeId, -1));
        if (specificListener != null) {
            log.debug("Calling processPacket.specificListener (" + specificListener + ") for: " + packet);
            specificListener.packetReceived(packet);
        }

        for (PacketReceivedListener e : listeners) {
            e.packetReceived(packet);
        }

    }

    public void addReceivedPacketListener(PacketReceivedListener listener) {
        log.debug("addReceivedPacketListener: " + listener);
        listeners.add(listener);
        listener.notifyRegistered(this);
    }

    public void addSpecificReceivedPacketListener(PacketReceivedListener listener, int nodeId, int messageType) {
        String key = createSpecificListenerKey(nodeId, messageType);
        log.debug("addSpecificReceivedPacketListener: " + listener + "for " + key);
        specificListeners.put(key, listener);
        listener.notifyRegistered(this);
    }

    private String createSpecificListenerKey(int nodeId, int messageType) {
        return nodeId + "@" + ((messageType < 0) ? "*" : MessageType.toString(messageType));
    }

    public void send(Packet packet) throws IOException {
        msgLog.debug(" < " + packet);
        packetSerializer.writePacket(packet, serialPort.getOutputStream());
    }

    public Packet send(Packet packet, int responseType, int timeout) throws IOException {
        log.debug("send(" + packet + ", " + MessageType.toString(responseType) + ")");
        ResponseWrapper responseWrapper = new ResponseWrapper(packet.nodeId, responseType);
        long begin = new Date().getTime();
        send(packet);
        Packet response = responseWrapper.waitForResponse(timeout);
        log.debug("resp (in " + (new Date().getTime() - begin) + " of " + timeout + "ms) " + response);
        if (response == null) log.warn("No response for " + packet + ", " + MessageType.toString(responseType) + ")");
        return response;
    }


    public void close() {
        if (serialPort != null) {
            serialPort.close();
            log.debug("Serial port listener closed");
        }
    }
}
