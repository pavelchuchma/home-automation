package org.chuma.homecontroller.app.servlet.pages;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.eclipse.jetty.server.Request;

public abstract class AbstractPage implements Page {
    final String rootPath;
    final String title;
    final String referenceName;
    final String favicon;

    public AbstractPage(String rootPath, String title, String referenceName, String favicon) {
        this.rootPath = rootPath;
        this.title = title;
        this.referenceName = referenceName;
        this.favicon = favicon;
    }

    @Override
    public String getRootPath() {
        return rootPath;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String getReferenceName() {
        return referenceName;
    }

    public String getFavicon() {
        return favicon;
    }

    String getHtmlHead() {
        return "<html><head>" +
                "<link rel='icon' type='image/png' href='" + getFavicon() + "'>" +
                "<link href='report.css' rel='stylesheet' type='text/css'/>" +
                "</head>";
    }

    void sendOkResponse(Request baseRequest, HttpServletResponse response, String body) throws IOException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        response.getWriter().println(body);
    }

    void initJsonResponse(Request baseRequest, HttpServletResponse response) {
        response.setContentType("application/json;charset=utf-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
    }

    void appendNameValue(StringBuffer b, String name, String value) {
        b.append("\"").append(name).append("\":\"").append(value).append("\"");
    }

    <T> T getItemById(HttpServletRequest request, Map<String, T> map) {
        String id = request.getParameter("id");
        Validate.notNull(id, "no id specified");
        T item = map.get(id);
        Validate.notNull(item, "unknown id");
        return item;
    }

    int tryTargetMatchAndParseArg(String target, String pattern) {
        if (target.startsWith(pattern)) {
            try {
                return Integer.parseInt(target.substring(pattern.length()));
            } catch (NumberFormatException e) {
                //ignore
            }
        }
        return -1;
    }
}
