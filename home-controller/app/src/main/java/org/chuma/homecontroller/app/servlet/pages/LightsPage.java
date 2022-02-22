package org.chuma.homecontroller.app.servlet.pages;

import java.util.List;

import static org.chuma.homecontroller.app.servlet.Servlet.currentValueFormatter;

import org.chuma.homecontroller.controller.actor.PwmActor;

public class LightsPage extends AbstractPage {
    final List<PwmActor> pwmActors;

    public LightsPage(List<PwmActor> pwmActors) {
        super("/lights", "Světla", "Světla", "favicon.png");
        this.pwmActors = pwmActors;
    }

    @Override
    public void appendContent(StringBuilder builder) {
        builder.append("<a href='")
                .append(getRootPath()).append("'>Refresh</a>&nbsp;&nbsp;&nbsp;&nbsp;<a href='/'>Back</a>\n");

        builder.append("<br/><br/><table class='buttonTable'>");
        for (PwmActor actor : pwmActors) {
            builder.append("<tr>");
            String stateFieldClass = (actor.isOn()) ? "louversRunning" : "louvers";
            appendLightAction(builder, "On", actor.getId(), "on");
            appendLightAction(builder, "➕", actor.getId(), "plus");
            builder.append(String.format("<td title='%s, max %s A' class='%s'>%s %.2f%% <div class='gray'>(%d/%d) %sA</div>",
                    actor.getLddOutput().getDeviceName(), currentValueFormatter.format(actor.getMaxOutputCurrent()),
                    stateFieldClass, actor.getLabel(), actor.getValue() * 100, actor.getCurrentPwmValue(), actor.getMaxPwmValue(),
                    currentValueFormatter.format(actor.getOutputCurrent())));
            appendLightAction(builder, "➖", actor.getId(), "minus");
            appendLightAction(builder, "Off", actor.getId(), "off");
            builder.append('\n');
        }
        builder.append("</table>");
    }

    private void appendLightAction(StringBuilder builder, String label, String id, String action) {
        builder.append("<td id='act_").append(id).append('_').append(action).append("' onClick=\"handleClick('").append(id).append("', '").append(action)
                .append("')\" class='").append("lightAction").append("'>").append(label).append("\n");
    }
}
