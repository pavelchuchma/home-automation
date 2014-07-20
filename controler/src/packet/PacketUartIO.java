package packet;

import gnu.io.*;
import node.MessageType;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PacketUartIO implements SerialPortEventListener, IPacketUartIO {
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

    protected List<PacketReceivedListener> receivedListeners = new ArrayList<PacketReceivedListener>();
    protected ConcurrentHashMap<String, PacketReceivedListener> specificReceivedListeners = new ConcurrentHashMap<String, PacketReceivedListener>();
    protected List<PacketSentListener> sentListeners = new ArrayList<PacketSentListener>();

    SerialPort serialPort;
    InputStream inputStream;
    PacketSerializer packetSerializer = new PacketSerializer();
    ExecutorService threadPool = Executors.newFixedThreadPool(20);

    public PacketUartIO(String portName, int baudRate) throws PacketUartIOException {
        log.debug("Creating '" + portName + "' @" + baudRate + " bauds...");
        Enumeration portList = CommPortIdentifier.getPortIdentifiers();

        while (portList.hasMoreElements()) {
            CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                if (portId.getName().equals(portName)) {
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

    void processPacket(final Packet packet) {
        if (packet.messageType == MessageType.MSG_OnHeartBeat) {
            msgLog.trace(" > " + packet);
        } else {
            msgLog.debug(" > " + packet);
        }

        // process each packet in new thread
        // todo: replace threads by producer/consumer pattern
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                processPacketImpl(packet);
            }
        });
    }

    private void processPacketByListener(Packet packet, PacketReceivedListener listener, String listenerType) {
        if (listener != null) {
            if (log.isDebugEnabled()) {
                String msg = String.format("Calling processPacket.%s (%s) for: %s", listenerType, listener, packet);
                if (packet.messageType == MessageType.MSG_OnHeartBeat) {
                    log.trace(msg);
                } else {
                    log.debug(msg);
                }
            }
            listener.packetReceived(packet);
        }
    }


    private void processPacketImpl(Packet packet) {
        // callbacks for nodeId + messageType
        PacketReceivedListener specificListener = specificReceivedListeners.get(createSpecificListenerKey(packet.nodeId, packet.messageType));
        processPacketByListener(packet, specificListener, "listenerNodeAndType");

        // callbacks for nodeId + all message types
        specificListener = specificReceivedListeners.get(createSpecificListenerKey(packet.nodeId, -1));
        processPacketByListener(packet, specificListener, "listenerNode");

        // callbacks for all messages
        for (PacketReceivedListener listener : receivedListeners) {
            processPacketByListener(packet, listener, "listenerAll");
        }

        //log.debug(String.format(" done: processPacketImpl for %s", packet));
    }

    @Override
    public void addReceivedPacketListener(PacketReceivedListener listener) {
        log.debug("addReceivedPacketListener: " + listener);
        // create a new copy to be thread safe
        List<PacketReceivedListener> tmp = new ArrayList<PacketReceivedListener>();
        tmp.addAll(receivedListeners);
        tmp.add(listener);

        receivedListeners = tmp;
        listener.notifyRegistered(this);
    }

    @Override
    public void addSpecificReceivedPacketListener(PacketReceivedListener listener, int nodeId, int messageType) {
        String key = createSpecificListenerKey(nodeId, messageType);
        log.debug("addSpecificReceivedPacketListener: " + listener + " for " + key);
        specificReceivedListeners.put(key, listener);
        listener.notifyRegistered(this);
    }

    @Override
    public void addSentPacketListener(PacketSentListener listener) {
        log.debug("addSentPacketListener: " + listener);
        sentListeners.add(listener);
    }

    private String createSpecificListenerKey(int nodeId, int messageType) {
        return nodeId + "@" + ((messageType < 0) ? "*" : MessageType.toString(messageType));
    }

    @Override
    public void send(Packet packet) throws IOException {
        msgLog.debug(" < " + packet);
        packetSerializer.writePacket(packet, serialPort.getOutputStream());
        for (PacketSentListener listener : sentListeners) {
            listener.packetSent(packet);
        }
    }

    @Override
    public Packet send(Packet packet, int responseType, int timeout) throws IOException {
        log.debug("send(" + packet + ", " + MessageType.toString(responseType) + ")");
        ResponseWrapper responseWrapper = new ResponseWrapper(packet.nodeId, responseType);
        long begin = new Date().getTime();
        send(packet);
        Packet response = responseWrapper.waitForResponse(timeout);
        log.debug("resp (in " + (new Date().getTime() - begin) + " of " + timeout + "ms) " + response);
        if (response == null) log.error("No response for " + packet + ", " + MessageType.toString(responseType) + ")");
        return response;
    }


    @Override
    public void close() {
        if (serialPort != null) {
            serialPort.close();
            log.debug("Serial port listener closed");
        }
    }
}
