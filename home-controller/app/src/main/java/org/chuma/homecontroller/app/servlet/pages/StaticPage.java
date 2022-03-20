package org.chuma.homecontroller.app.servlet.pages;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.server.Request;

import org.chuma.homecontroller.app.servlet.Servlet;

public class StaticPage implements Page {
    final static Map<String, String> fileExtensionToContentType = buildFileTypeMap();
    final String path;
    final String resourcePath;
    final String linkTitle;

    public StaticPage(String path, String resourcePath, String linkTitle) {
        this.path = (path.startsWith("/")) ? path : '/' + path;
        this.linkTitle = linkTitle;
        this.resourcePath = resourcePath;
    }

    private static Map<String, String> buildFileTypeMap() {
        Map<String, String> m = new HashMap<>();
        m.put(".css", "text/css;charset=utf-8");
        m.put(".html", "text/html;charset=utf-8");
        m.put(".jpg", "image/jpeg");
        m.put(".png", "image/png");
        m.put(".js", "application/javascript;charset=utf-8");
        return m;
    }

    private static String getTargetExtension(String target) {
        int i = target.lastIndexOf('.');
        return (i > 0) ? target.substring(i) : null;
    }

    public static boolean sendFile(String target, HttpServletResponse response) throws IOException {
        if (target.contains(":") || target.contains("..")) {
            // suspicious path
            return false;
        }
        String extension = getTargetExtension(target);
        if (extension == null) {
            // no extension?! -> not supported
            return false;
        }
        String contentType = fileExtensionToContentType.get(extension);
        if (contentType == null) {
            // unknown extension
            return false;
        }
        InputStream in = Servlet.class.getResourceAsStream("/servlet/content" + target);
        if (in == null) {
            // not found
            return false;
        }
        response.setContentType(contentType);
        byte[] buff = new byte[16384];
        int read;
        while ((read = in.read(buff)) > 0) {
            response.getOutputStream().write(buff, 0, read);
        }
        return true;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void handle(String target, Request request, HttpServletResponse response) throws IOException {
        sendFile(resourcePath, response);
    }

    @Override
    public String getLinkTitle() {
        return linkTitle;
    }
}
