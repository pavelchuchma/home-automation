package org.chuma.homecontroller.app.servlet.ws;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class WebSocketServletImpl extends WebSocketServlet {
    private WebSocketHandler handler;

    public WebSocketServletImpl(WebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void configure(WebSocketServletFactory factory) {
        // Normally, we would just register class to factory using factory.register(SomeAdapter.class)
        // However, this does not allow injecting parameters. Therefore we set own creator instead.
        // In that case, there is no need to register the class since we effectively override all
        // instantiation in WebSocketServletFactory (in case it is default...)
        factory.setCreator((req, resp) -> handler.newAdapter());
    }
}
