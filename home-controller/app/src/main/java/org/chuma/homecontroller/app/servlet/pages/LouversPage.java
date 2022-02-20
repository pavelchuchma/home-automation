package org.chuma.homecontroller.app.servlet.pages;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.eclipse.jetty.server.Request;

import org.chuma.homecontroller.controller.controller.LouversController;

public class LouversPage extends AbstractPage {
    public static final String CLASS_LOUVERS_ARROW = "louversArrow";
    final LouversController[] louversControllers;

    public LouversPage(LouversController[] louversControllers) {
        super("/louvers", "Žaluzie", "Žaluzie", "favicon.png");
        this.louversControllers = louversControllers;
    }

    @Override
    String getHtmlHead() {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Louvers Control</title>\n" +
                "    <link href='louvers.css' rel='stylesheet' type='text/css'/>\n" +
                "    <link href=\"favicon.png\" rel=\"icon\" type=\"image/png\">\n" +
                "    <script src=\"commons.js\"></script>\n" +
                "    <script src=\"status.js\"></script>\n" +
                "    <script src=\"items/baseItem.js\"></script>\n" +
                "    <script src=\"items/louversItem.js\"></script>\n" +
                "    <script src=\"configuration-pi.js\"></script>\n" +
                "    <script src=\"louvers.js\"></script>\n" +
                "</head>";
    }

    public String getBody() {
        StringBuilder builder = new StringBuilder();

        builder.append("<html>")
                .append(getHtmlHead())
                .append("<body><p id='error'></p><a href='")
                .append(getRootPath()).append("'>Refresh</a>&nbsp;&nbsp;&nbsp;&nbsp;<a href='/'>Back</a>\n");

        for (int i = 0; i < louversControllers.length; i += 4) {
            int count = (i + 8 < louversControllers.length) ? 4 : louversControllers.length - i;
            builder.append(getLouversTable(i, count));
            if (count > 4) {
                break;
            }
        }

        builder.append("</body></html>");
        return builder.toString();
    }

    private String getLouversTable(int startIndex, int count) {
        StringBuilder builder = new StringBuilder();
        builder.append("<br/><br/><table class='buttonTable'>");
        for (int i = startIndex; i < startIndex + count; i++) {
            LouversController lc = louversControllers[i];

            builder.append("<td id='").append(lc.getId()).append("' class='louversItem'>\n" +
                    "<table>\n");

            appendLouversIcon(builder, "?", lc.getId(), "up");
            appendLouversIcon(builder, "☀", lc.getId(), "outshine");
            appendLouversIcon(builder, "?", lc.getId(), "blind");

            builder.append("<tr><td colspan='3' class='louversName'>").append(lc.getLabel()).append("<tr>\n</table>\n");
        }
        builder.append("</table>");
        return builder.toString();
    }

    private void appendLouversIcon(StringBuilder builder, String icon, String id, String action) {
        builder.append("<td id='act_").append(id).append('_').append(action).append("' onClick=\"handleClick('").append(id).append("', '").append(action)
                .append("')\" class='").append(CLASS_LOUVERS_ARROW).append("'>").append(icon).append("\n");
    }

    @Override
    public void handle(String target, Request request, HttpServletResponse response) throws IOException {
        sendOkResponse(request, response, getBody());
    }
}
