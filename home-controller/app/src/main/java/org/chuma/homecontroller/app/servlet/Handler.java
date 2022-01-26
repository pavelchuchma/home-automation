package org.chuma.homecontroller.app.servlet;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.eclipse.jetty.server.Request;

public interface Handler {
    String getRootPath();

    void handle(String target, Request request, HttpServletResponse response) throws IOException;
}
