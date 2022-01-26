package org.chuma.homecontroller.app.servlet.pages;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import org.eclipse.jetty.server.Request;
import static org.chuma.homecontroller.app.servlet.Servlet.currentValueFormatter;

import org.chuma.homecontroller.controller.action.Action;
import org.chuma.homecontroller.controller.actor.PwmActor;

public class LightsPage extends AbstractPage {
    public static final String TARGET_LIGHTS = "/lights";
    public static final String TARGET_LIGHTS_STATUS = TARGET_LIGHTS + "/status";
    public static final String TARGET_LIGHTS_ACTION = TARGET_LIGHTS + "/a";
    public static final String TARGET_LIGHTS_PARAM_ACTION = TARGET_LIGHTS + "/action";
    final List<Action> lightActions;

    public LightsPage(List<Action> lightActions) {
        super(TARGET_LIGHTS, "Světla", "Světla", "favicon.png");
        this.lightActions = lightActions;
    }

    public String getBody() {
        StringBuilder builder = new StringBuilder();

        builder.append("<html>").append(getHtmlHead()).append("<body><a href='")
                .append(getRootPath()).append("'>Refresh</a>&nbsp;&nbsp;&nbsp;&nbsp;<a href='/'>Back</a>\n");

        builder.append("<br/><br/><table class='buttonTable'>");
        for (int i = 0; i < lightActions.size(); i += 4) {
            builder.append("<tr>");
            PwmActor actor = (PwmActor) lightActions.get(i).getActor();
            String fieldClass = "louvers";
            String stateFieldClass = (actor.isOn()) ? "louversRunning" : "louvers";
            builder.append(String.format("<td class='%s'><a href='%s%d'>%s</a>", fieldClass, TARGET_LIGHTS_ACTION, i, "On"));
            builder.append(String.format("<td class='%s'><a href='%s%d'>%s</a>", fieldClass, TARGET_LIGHTS_ACTION, i + 1, "+"));
            builder.append(String.format("<td title='%s, max %s A' class='%s'>%s %d%% <div class='gray'>(%d/%d) %sA</div>",
                    actor.getLddOutput().getDeviceName(), currentValueFormatter.format(actor.getMaxOutputCurrent()),
                    stateFieldClass, lightActions.get(i).getActor().getLabel(), actor.getValue(), actor.getPwmValue(), actor.getMaxPwmValue(),
                    currentValueFormatter.format(actor.getOutputCurrent())));
            builder.append(String.format("<td class='%s'><a href='%s%d'>%s</a>", fieldClass, TARGET_LIGHTS_ACTION, i + 2, "-"));
            builder.append(String.format("<td class='%s'><a href='%s%d'>%s</a>", fieldClass, TARGET_LIGHTS_ACTION, i + 3, "Off"));
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
