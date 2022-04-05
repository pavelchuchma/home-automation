package org.chuma.homecontroller.app.servlet.ws;

import org.eclipse.jetty.websocket.api.WebSocketAdapter;

/**
 * Handler for web socket connections.
 */
public interface WebSocketHandler {
    /**
     * Path to handle. Handles all requests that are targeted directly to this path
     * or which have this path as parent.
     */
    String getPath();

    /**
     * Create web socket adapter for new connection.
     */
    WebSocketAdapter newAdapter();
}
