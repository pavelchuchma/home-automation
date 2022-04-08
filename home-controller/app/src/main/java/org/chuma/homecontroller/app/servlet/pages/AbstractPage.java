package org.chuma.homecontroller.app.servlet.pages;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.jetty.server.Request;

public abstract class AbstractPage implements Page {
    public static final String VIRTUAL_CONFIGURATION_JS_FILENAME = "configuration.js";
    final String path;
    final String title;
    final String linkTitle;
    final String favicon;
    final Iterable<Page> links;

    public AbstractPage(String path, String title, String linkTitle, String favicon, Iterable<Page> links) {
        this.path = path;
        this.title = title;
        this.linkTitle = linkTitle;
        this.favicon = favicon;
        this.links = (links != null) ? links : new ArrayList<>();
    }

    @Override
    public String getPath() {
        return path;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String getLinkTitle() {
        return linkTitle;
    }

    public String getFavicon() {
        return favicon;
    }

    public String[] getStylesheets() {
        return new String[]{"report.css"};
    }

    public String[] getScripts() {
        return new String[]{};
    }

    StringBuilder beginHtlDocument() {
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset='UTF-8'>\n");
        builder.append("    <title>").append(getTitle()).append("</title>\n" +
                "    <link href='/").append(getFavicon()).append("' rel='icon' type='image/png'>\n");
        for (String stylesheet : getStylesheets()) {
            builder.append("    <link href='/").append(stylesheet).append("' rel='stylesheet' type='text/css'/>\n");
        }
        for (String script : getScripts()) {
            builder.append("    <script src='/").append(script).append("'></script>\n");
        }
        builder.append("</head>\n<body>\n<p id='error'></p>\n");

        for (Page page : links) {
            if (page != this) {
                builder.append("<a href='").append(page.getPath()).append("'>");
            }
            builder.append(page.getLinkTitle()).append("...");
            if (page != this) {
                builder.append("</a>");
            }
            builder.append("&nbsp;&nbsp;&nbsp;&nbsp;");
        }
        if (links.iterator().hasNext()) {
            builder.append("<br/>");
        }
        return builder;
    }

    void sendOkResponse(Request baseRequest, HttpServletResponse response, String body) throws IOException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        response.getWriter().println(body);
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

    protected abstract void appendContent(StringBuilder builder, Map<String, String[]> requestParameters);

    @Override
    public void handle(String target, Request request, HttpServletResponse response) throws IOException {
        StringBuilder builder = beginHtlDocument();
        appendContent(builder, request.getParameterMap());
        builder.append("</body></html>");
        sendOkResponse(request, response, builder.toString());
    }
}
