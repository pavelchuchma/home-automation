package org.chuma.homecontroller.app.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DecimalFormat;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.app.servlet.pages.StaticPage;

public class Servlet extends AbstractHandler {
    public static DecimalFormat currentValueFormatter = new DecimalFormat("###.##");
    static Logger log = LoggerFactory.getLogger(Servlet.class.getName());
    final private Iterable<Handler> handlers;
    final private String defaultPath;

    public Servlet(Iterable<Handler> handlers, String defaultPage) {
        this.handlers = handlers;
        this.defaultPath = defaultPage;
    }

    public static void startServer(Servlet servlet) throws Exception {
        if (servlet == null) {
            throw new IllegalArgumentException("servlet is null");
        }
        log.info("Starting web server");
        Server server = new Server(Integer.parseInt(System.getProperty("servlet.port", "80")));
        server.setHandler(servlet);

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