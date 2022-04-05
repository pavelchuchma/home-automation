package org.chuma.homecontroller.app.servlet;

import java.io.IOException;
import java.text.DecimalFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.Validate;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.app.servlet.pages.StaticPage;
import org.chuma.homecontroller.app.servlet.ws.WebSocketHandler;
import org.chuma.homecontroller.app.servlet.ws.WebSocketServletImpl;

/**
 * Jetty URL handler dispatching requests to registered {@link Handler}s and serving static pages via {@link StaticPage#sendFile(String, HttpServletResponse)}.
 */
public class Servlet extends AbstractHandler {
    public static DecimalFormat currentValueFormatter = new DecimalFormat("###.##");
    static Logger log = LoggerFactory.getLogger(Servlet.class.getName());
    final private Iterable<Handler> handlers;
    final private String defaultPath;
    final private Iterable<WebSocketHandler> wsHandlers;

    /**
     * Create servlet with given URL handlers and redirect URL if no handler found.
     *
     * @param handlers    handlers for HTTP requests, bound directly to root context
     * @param defaultPage default page to redirect to when no handler is found
     * @param wsHandlers  web socket handlers, these are bound in context path "/web-socket"
     */
    public Servlet(Iterable<Handler> handlers, String defaultPage, Iterable<WebSocketHandler> wsHandlers) {
        Validate.noNullElements(handlers);
        Validate.noNullElements(wsHandlers);
        this.handlers = handlers;
        this.defaultPath = defaultPage;
        this.wsHandlers = wsHandlers;
    }

    /**
     * Start HTTP server with given servlet as request handler. Server runs on port
     * specified in {@code servlet.port} property or on port 80.
     */
    public static void startServer(Servlet servlet) throws Exception {
        if (servlet == null) {
            throw new IllegalArgumentException("servlet is null");
        }
        log.info("Starting web server");
        Server server = new Server(Integer.parseInt(System.getProperty("servlet.port", "80")));

        // Prepare handler for web sockets and servlets for configured wsHandlers
        ServletContextHandler wsHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        wsHandler.setContextPath("/web-socket");
        for (WebSocketHandler h : servlet.wsHandlers) {
            wsHandler.addServlet(new ServletHolder(new WebSocketServletImpl(h)), h.getPath());
        }
        // Prepare handler list - wsHandler first since servlet handles all the rest
        HandlerList handlerList = new HandlerList();
        handlerList.addHandler(wsHandler);
        handlerList.addHandler(servlet);
        server.setHandler(handlerList);

        server.start();
        log.info("Web server started");
        server.join();
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
        try {
            log.debug("handle: " + target);
            for (Handler handler : handlers) {
                if (handler.getPath().equals(target) || target.startsWith(handler.getPath() + "/") || target.startsWith(handler.getPath() + "?")) {
                    handler.handle(target, baseRequest, response);
                    return;
                }
            }
            if (!StaticPage.sendFile(target, response)) {
                response.sendRedirect(defaultPath);
            }
        } catch (Exception e) {
            log.error("failed to process '" + target + "'", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace(response.getWriter());
        } finally {
            baseRequest.setHandled(true);
        }
    }
}