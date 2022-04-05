package org.chuma.homecontroller.app.servlet.ws;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for WebSockerAdapter implementation. A new instance of adapter is created
 * for each new websocket connection.
 */
public class AbstractWebSocketAdapter extends WebSocketAdapter {
    private static final Logger log = LoggerFactory.getLogger(AbstractWebSocketAdapter.class.getName());

    protected Session session;

    @Override
    public void onWebSocketConnect(Session session) {
        super.onWebSocketConnect(session); 
        this.session = session;
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        this.session = null;
        super.onWebSocketClose(statusCode, reason); 
    }
    
    /**
     * Send message to client.
     */
    public void sendText(String message) {
        if (session != null && session.isOpen()) {
            try {
                session.getRemote().sendString(message);
            } catch (Exception e) {
                log.warn("Failed to sent websocket message", e);
            }
        }
    }
}
