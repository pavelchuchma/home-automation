package org.chuma.homecontroller.app.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.app.servlet.pages.Page;

public class Servlet extends AbstractHandler {
    public static DecimalFormat currentValueFormatter = new DecimalFormat("###.##");
    static Logger log = LoggerFactory.getLogger(Servlet.class.getName());

    final String configurationJs;
    final Map<String, String> fileExtensionToContentType;
    final private Iterable<Handler> handlers;
    final private Page defaultPage;

    public Servlet(Iterable<Handler> handlers, Page defaultPage, String configurationJs) {
        this.handlers = handlers;
        this.defaultPage = defaultPage;
        this.configurationJs = configurationJs;
        fileExtensionToContentType = buildFileTypeMap();
    }

    public static void startServer(Servlet servlet) throws Exception {
        log.info("Starting web server");
        Server server = new Server(80);
        server.setHandler(servlet);

        server.start();
        log.info("Web server started");
        server.join();
    }

    private Map<String, String> buildFileTypeMap() {
        Map<String, String> m = new HashMap<>();
        m.put(".css", "text/css;charset=utf-8");
        m.put(".html", "text/html;charset=utf-8");
        m.put(".jpg", "image/jpeg");
        m.put(".png", "image/png");
        m.put(".js", "application/javascript;charset=utf-8");
        return m;
    }

    public void handle(String target, Request baseRequest, HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
        try {
            log.debug("handle: " + target);
            for (Handler handler : handlers) {
                if (handler.getRootPath().equals(target) || target.startsWith(handler.getRootPath() + "/") || target.startsWith(handler.getRootPath() + "?")) {
                    handler.handle(target, baseRequest, response);
                    return;
                }
            }

            String extension = getTargetExtension(target);
            if (extension != null) {
                String contentType = fileExtensionToContentType.get(extension);
                if (contentType != null) {
                    sendFile(target, response, contentType);
                    return;
                }
            }
            defaultPage.handle(target, baseRequest, response);
        } catch (Exception e) {
            log.error("failed to process '" + target + "'", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace(response.getWriter());
        } finally {
            baseRequest.setHandled(true);
        }
    }

    private String getTargetExtension(String target) {
        int i = target.lastIndexOf('.');
        return (i > 0) ? target.substring(i) : null;
    }

    private void sendFile(String target, HttpServletResponse response, String contentType) throws IOException {
        if (!target.contains(":") && !target.contains("..")) {
            response.setContentType(contentType);

            InputStream in = this.getClass().getResourceAsStream("/servlet/content" + target);
            if (in != null) {
                if (target.endsWith(".html")) {
                    Charset charset = StandardCharsets.UTF_8;
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), charset));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        writer.write(line.replace("configuration-pi.js", configurationJs));
                        writer.write('\n');
                    }
                    writer.close();
                } else {
                    byte[] buff = new byte[1024];
                    int read;
                    while ((read = in.read(buff)) > 0) {
                        response.getOutputStream().write(buff, 0, read);
                    }
                }
                return;
            }
        }
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
}