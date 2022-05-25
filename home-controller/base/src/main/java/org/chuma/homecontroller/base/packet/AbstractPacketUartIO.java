package org.chuma.homecontroller.base.packet;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.node.ListenerManager;
import org.chuma.homecontroller.base.node.MessageType;

/**
 * Abstract base class for {@link IPacketUartIO} implementations. Provides listener registrations
 * and packet dispatching to listeners. Subclasses need to implement {@link #sendImpl(Packet)}
 * to actually send the message and {@link #start()}, {@link #close()} methods to initialize and
 * terminate the reception loop. Received messages should be passed to {@link #processPacket(Packet)}
 * to log them and deliver them to listeners.
 */
public abstract class AbstractPacketUartIO implements IPacketUartIO {
    protected static Logger log = LoggerFactory.getLogger(PacketUartIO.class.getName());
    protected static Logger msgLog = LoggerFactory.getLogger(PacketUartIO.class.getName() + ".msg");

    private final ListenerManager<PacketReceivedListener> receivedListeners = new ListenerManager<>();
    private final ConcurrentHashMap<String, PacketReceivedListener> specificReceivedListeners = new ConcurrentHashMap<>();
    private final ListenerManager<PacketSentListener> sentListeners = new ListenerManager<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Log and process received packet by registered listeners in separate thread.
     */
    protected void processPacket(final Packet packet) {
        if (packet.messageType == MessageType.MSG_OnHeartBeat) {
            msgLog.trace(" > " + packet);
        } else {
            msgLog.debug(" > " + packet);
        }

        // process each packet in new thread
        // todo: replace threads by producer/consumer pattern
        executor.execute(() -> processPacketImpl(packet));
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
        receivedListeners.callListeners(listener -> processPacketByListener(packet, listener, "listenerAll"));
    }

    @Override
    public void addReceivedPacketListener(PacketReceivedListener listener) {
        log.debug("addReceivedPacketListener: {}", listener);
        receivedListeners.add(listener);
        listener.notifyRegistered(this);
    }

    // TODO: Unlike generic listener, this one replaces existing registered listener - should be setSpecificReceivedPacketListener, not add...
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

    /**
     * Real implementation of send. Called while PacketUartIO instance is locked.
     * Any registered send handlers are called automatically after send.
     */
    protected abstract void sendImpl(Packet packet) throws IOException;

    @Override
    public void send(Packet packet) throws IOException {
        synchronized (this) {
            msgLog.debug(" < {}", packet);
            sendImpl(packet);
        }
        sentListeners.callListeners(listener -> listener.packetSent(packet));
    }

    @Override
    public Packet send(Packet packet, int responseType, int timeout) throws IOException {
        log.debug("send({}, {})", packet, MessageType.toString(responseType));
        ResponseWrapper responseWrapper = new ResponseWrapper(packet.nodeId, responseType);
        long begin = System.currentTimeMillis();
        send(packet);

        // hack to force output write
//        send(Packet.createMsgEchoRequest(49, 1, 2));

        // TODO: This will kill any existing (user-defined) specific listener. Also remains registered when finished - although none can use it any more
        Packet response = responseWrapper.waitForResponse(timeout);
        log.debug("resp (in {} of {}ms) {}", (System.currentTimeMillis() - begin), timeout, response);
        if (response == null) log.error("No response for {}, {}", packet, MessageType.toString(responseType));
        return response;
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
        public void notifyRegistered(IPacketUartIO packetUartIO) {

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
