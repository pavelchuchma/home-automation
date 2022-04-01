package org.chuma.homecontroller.base.packet;

import java.io.IOException;

public interface IPacketUartIO {
    /**
     * Register listener for any message received.
     */
    void addReceivedPacketListener(PacketReceivedListener listener);

    /**
     * Register listener for specific message type received from given node. There can be only one such listener registered
     * for particular combination of {@code nodeId} and {@code messageType}.
     *
     * @param nodeId node ID to receive message from
     * @param messageType message type to receive or -1 for any
     */
    void addSpecificReceivedPacketListener(PacketReceivedListener listener, int nodeId, int messageType);

    /**
     * Register listener called after the message was successfully sent.
     */
    void addSentPacketListener(PacketSentListener listener);

    /**
     * Send packet.
     */
    void send(Packet packet) throws IOException;

    /**
     * Send packet and wait for response of given type. Uses {@link #addSpecificReceivedPacketListener(PacketReceivedListener, int, int)}
     * to handle the response so it will discard any listener already registered for packet node ID and {@code responseType}.
     *
     * @return received response or null if not received within timeout
     */
    Packet send(Packet packet, int responseType, int timeout) throws IOException;

    /**
     * Start to receive and process messages.
     */
    void start() throws IOException;

    /**
     * Stop processing.
     */
    void close();

    /**
     * Listener called for received message.
     */
    public interface PacketReceivedListener {
        /**
         * Called when message was received.
         */
        void packetReceived(Packet packet);

        /**
         * Called when listener was registered in {@link IPacketUartIO}.
         */
        void notifyRegistered(IPacketUartIO packetUartIO);
    }

    /**
     * Listener called after message was successfully sent.
     */
    public interface PacketSentListener {
        void packetSent(Packet packet);
    }
}
