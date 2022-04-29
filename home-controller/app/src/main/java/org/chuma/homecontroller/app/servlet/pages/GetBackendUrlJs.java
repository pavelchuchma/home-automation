package org.chuma.homecontroller.app.servlet.pages;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.eclipse.jetty.server.Request;

import org.chuma.homecontroller.app.configurator.Options;
import org.chuma.homecontroller.app.configurator.OptionsSingleton;
import org.chuma.homecontroller.app.servlet.Handler;

public class GetBackendUrlJs implements Handler {
    public static final String PATH = "getBackendUrl.js";
    private static final String DEFAULT_BACKED_URL_PROPERTY = "default.backed.url";

    @Override
    public String getPath() {
        return '/' + PATH;
    }

    void appendGetBaseUrlFunction(StringBuilder builder) {
        builder.append("function getBaseUrl() {");
        Options options = OptionsSingleton.getInstance();
        String baseUrl = (options != null) ? options.get(DEFAULT_BACKED_URL_PROPERTY) : null;
        if (baseUrl != null) {
            builder.append("return '").append(baseUrl).append("';");
        } else {
            builder.append("const s = new URL('/', new URL(window.location.href)).toString();" +
                    "return s.substring(0, s.length - 1);");
        }
        builder.append("}");
    }

    @Override
    public void handle(String target, Request request, HttpServletResponse response) throws IOException {
        StringBuilder builder = new StringBuilder();
        appendGetBaseUrlFunction(builder);

        response.setContentType("application/javascript;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        request.setHandled(true);
        response.getWriter().print(builder);
    }
}
