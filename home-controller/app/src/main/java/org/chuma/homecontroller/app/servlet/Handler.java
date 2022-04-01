package org.chuma.homecontroller.app.servlet;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.eclipse.jetty.server.Request;

/**
 * HTTP URL handler.
 */
public interface Handler {
    /**
     * Path to handle. Handles all requests that are targeted directly to this path
     * or which have this path as parent.
     */
    String getPath();
    /**
     * Handle request.
     */
    void handle(String target, Request request, HttpServletResponse response) throws IOException;
}
