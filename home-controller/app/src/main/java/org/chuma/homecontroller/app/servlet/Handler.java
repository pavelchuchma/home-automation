package org.chuma.homecontroller.app.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.eclipse.jetty.server.Request;

public interface Handler {
    String getRootPath();

    void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException;
}
