package org.chuma.homecontroller.app.servlet.ws;

public abstract class AbstractWebSocketHandler implements WebSocketHandler {
    private String path;

    public AbstractWebSocketHandler(String path) {
        this.path = path;
    }

    @Override
    public String getPath() {
        return path;
    }
}
