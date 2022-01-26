package org.chuma.homecontroller.app.servlet.pages;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import org.eclipse.jetty.server.Request;

import org.chuma.homecontroller.controller.PirStatus;

public class PirPage extends AbstractPage {
    public static final String TARGET_PIR = "/pir";
    public static final String TARGET_PIR_STATUS = TARGET_PIR + "/status";
    final List<PirStatus> pirStatusList;

    public PirPage(List<PirStatus> pirStatusList) {
        super(TARGET_PIR, "PIR Status", "PIR", "favicon.png");
        this.pirStatusList = pirStatusList;
    }

    public String getBody() {
        StringBuilder builder = new StringBuilder();

        builder.append("<html><meta http-equiv='refresh' content='1;url=").append(getRootPath()).append("'/>")
                .append(getHtmlHead()).append("<body><a href='").append(getRootPath())
                .append("'>Refresh</a>&nbsp;&nbsp;&nbsp;&nbsp;<a href='/'>Back</a>\n");

        builder.append("<br/><br/><table class='buttonTable'>");
        for (PirStatus status : pirStatusList) {
            builder.append("<tr>");
            String stateFieldClass = (status.isActive()) ? "louversRunning" : "louvers";
            builder.append(String.format("<td class='%s'>%s", stateFieldClass, status.getName()));
            String fieldClass = "louvers";
            builder.append(String.format("<td class='%s'>%s", fieldClass, status.getLastActivate()));
        }
        builder.append("</table>");

        builder.append("</body></html>");
        return builder.toString();

    }

    @Override
    public void handle(String target, Request request, HttpServletResponse response) throws IOException {
        sendOkResponse(request, response, getBody());
    }
}
