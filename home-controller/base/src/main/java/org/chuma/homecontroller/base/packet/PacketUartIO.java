package org.chuma.homecontroller.base.packet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.node.MessageType;

public class PacketUartIO implements IPacketUartIO {
    private static Logger log = LoggerFactory.getLogger(PacketUartIO.class.getName());
    private static Logger msgLog = LoggerFactory.getLogger(PacketUartIO.class.getName() + ".msg");

    private Collection<PacketReceivedListener> receivedListeners = new ConcurrentLinkedQueue<>();
    private ConcurrentHashMap<String, PacketReceivedListener> specificReceivedListeners = new ConcurrentHashMap<>();
    private Collection<PacketSentListener> sentListeners = new ConcurrentLinkedQueue<>();
    private SerialPort serialPort;
    private ExecutorService threadPool = Executors.newFixedThreadPool(40);
    private boolean closed = false;

    public PacketUartIO(String portName, int baudRate) throws PacketUartIOException {
        log.debug("Creating '{}' @{} bauds...", portName, baudRate);
        Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();

        while (portList.hasMoreElements()) {
            CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                if (portId.getName().equals(portName)) {
                    try {
                        serialPort = (SerialPort) portId.open("SimpleReadApp", 2000);
                        serialPort.notifyOnDataAvailable(false);
                        serialPort.setSerialPortParams(baudRate,
                                SerialPort.DATABITS_8,
                                SerialPort.STOPBITS_1,
                                SerialPort.PARITY_NONE);
                        serialPort.enableReceiveTimeout(3600000);
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

    public void start() throws IOException {
        InputStream inputStream = serialPort.getInputStream();
        new Thread(() -> {
            log.debug("Starting read com thread");
            PacketSerializer packetSerializer = new PacketSerializer();
            while (!closed) {
                try {
                    Packet receivedPacket = packetSerializer.readPacket(inputStream);
                    processPacket(receivedPacket);
                } catch (IOException e) {
                    msgLog.error("receiveError", e);
                    log.error("receiveError", e);
                    e.printStackTrace();
                }
            }
            log.debug("Ending read com thread");
        }, "ComRead").start();
    }

    void processPacket(final Packet packet) {
        if (packet.messageType == MessageType.MSG_OnHeartBeat) {
            msgLog.trace(" > " + packet);
        } else {
            msgLog.debug(" > " + packet);
        }

        // process each packet in new thread
        // todo: replace threads by producer/consumer pattern
        threadPool.execute(() -> processPacketImpl(packet));
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
    }

    @Override
    public void addReceivedPacketListener(PacketReceivedListener listener) {
        log.debug("addReceivedPacketListener: {}", listener);
        receivedListeners.add(listener);
        listener.notifyRegistered(this);
    }

    @Override
    public void addSpecificReceivedPacketListener(PacketReceivedListener listener, int nodeId, int messageType) {
        String key = createSpecificListenerKey(nodeId, messageType);
        log.debug("addSpecificReceivedPacketListener: {} for {}", listener, key);
        specificReceivedListeners.put(key, listener);
        listener.notifyRegistered(this);
    }

    @Override
    public void addSentPacketListener(PacketSentListener listener) {
        log.debug("addSentPacketListener: {}", listener);
        sentListeners.add(listener);
    }

    private String createSpecificListenerKey(int nodeId, int messageType) {
        return nodeId + "@" + ((messageType < 0) ? "*" : MessageType.toString(messageType));
    }

    @Override
    public void send(Packet packet) throws IOException {
        synchronized (this) {
            msgLog.debug(" < {}", packet);
            PacketSerializer.writePacket(packet, serialPort.getOutputStream());
        }
        for (PacketSentListener listener : sentListeners) {
            listener.packetSent(packet);
        }
    }

    @Override
    public Packet send(Packet packet, int responseType, int timeout) throws IOException {
        log.debug("send({}, {})", packet, MessageType.toString(responseType));
        ResponseWrapper responseWrapper = new ResponseWrapper(packet.nodeId, responseType);
        long begin = System.currentTimeMillis();
        send(packet);

        // hack to force output write
//        send(Packet.createMsgEchoRequest(49, 1, 2));

        Packet response = responseWrapper.waitForResponse(timeout);
        log.debug("resp (in {} of {}ms) {}", (System.currentTimeMillis() - begin), timeout, response);
        if (response == null) log.error("No response for {}, {}", packet, MessageType.toString(responseType));
        return response;
    }

    @Override
    public void close() {
        closed = true;
        if (serialPort != null) {
            serialPort.close();
            log.debug("Serial port listener closed");
        }
    }

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
}
