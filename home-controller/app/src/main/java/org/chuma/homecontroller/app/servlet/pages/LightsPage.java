package org.chuma.homecontroller.app.servlet.pages;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import org.eclipse.jetty.server.Request;
import static org.chuma.homecontroller.app.servlet.Servlet.currentValueFormatter;

import org.chuma.homecontroller.controller.actor.PwmActor;

public class LightsPage extends AbstractPage {
    public static final String TARGET_LIGHTS = "/lights";
    public static final String TARGET_LIGHTS_STATUS = TARGET_LIGHTS + "/status";
    public static final String TARGET_LIGHTS_ACTION = TARGET_LIGHTS + "/a";
    public static final String TARGET_LIGHTS_PARAM_ACTION = TARGET_LIGHTS + "/action";
    final List<PwmActor> pwmActors;

    public LightsPage(List<PwmActor> pwmActors) {
        super(TARGET_LIGHTS, "Světla", "Světla", "favicon.png");
        this.pwmActors = pwmActors;
    }

    public String getBody() {
        StringBuilder builder = new StringBuilder();

        builder.append("<html>").append(getHtmlHead()).append("<body><a href='")
                .append(getRootPath()).append("'>Refresh</a>&nbsp;&nbsp;&nbsp;&nbsp;<a href='/'>Back</a>\n");

        builder.append("<br/><br/><table class='buttonTable'>");
        for (PwmActor actor : pwmActors) {
            builder.append("<tr>");
            String fieldClass = "louvers";
            String stateFieldClass = (actor.isOn()) ? "louversRunning" : "louvers";
            builder.append(String.format("<td class='%s'><a href='%s%d'>%s</a>", fieldClass, TARGET_LIGHTS_ACTION, 0, "On"));
            builder.append(String.format("<td class='%s'><a href='%s%d'>%s</a>", fieldClass, TARGET_LIGHTS_ACTION, 1, "+"));
            builder.append(String.format("<td title='%s, max %s A' class='%s'>%s %.2f%% <div class='gray'>(%d/%d) %sA</div>",
                    actor.getLddOutput().getDeviceName(), currentValueFormatter.format(actor.getMaxOutputCurrent()),
                    stateFieldClass, actor.getLabel(), actor.getValue() * 100, actor.getCurrentPwmValue(), actor.getMaxPwmValue(),
                    currentValueFormatter.format(actor.getOutputCurrent())));
            builder.append(String.format("<td class='%s'><a href='%s%d'>%s</a>", fieldClass, TARGET_LIGHTS_ACTION, 2, "-"));
            builder.append(String.format("<td class='%s'><a href='%s%d'>%s</a>", fieldClass, TARGET_LIGHTS_ACTION, 3, "Off"));
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
