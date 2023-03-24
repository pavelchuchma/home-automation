package org.chuma.homecontroller.app.servlet.pages;

import java.util.List;
import java.util.Map;

import static org.chuma.homecontroller.app.servlet.Servlet.currentValueFormatter;

import org.chuma.homecontroller.controller.actor.LddActor;

public class LightsPage extends AbstractPage {
    final List<LddActor> lddActors;

    public LightsPage(List<LddActor> lddActors, Iterable<Page> links) {
        super("/lights", "Světla", "Světla", "favicon.png", links);
        this.lddActors = lddActors;
    }

    @Override
    public String[] getStylesheets() {
        return new String[]{"commons.css", "lights.css"};
    }

    @Override
    public String[] getScripts() {
        return new String[]{
                "commons.js",
                "status.js",
                "items/baseItem.js",
                "items/onOffItem.js",
                "items/pwmLightItem.js",
                GetBackendUrlJs.PATH,
                VIRTUAL_CONFIGURATION_JS_FILENAME,
                "lights.js"
        };
    }

    @Override
    public void appendContent(StringBuilder builder, Map<String, String[]> requestParameters) {
        builder.append("<br/><br/><table class='lightsTable'>");
        for (LddActor actor : lddActors) {
            builder.append("<tr>");
            final String id = actor.getId();
            appendClickableRow(builder, id, "toggle", "onOff").append("<img src='img/onOff.png' alt='on/off' class='onOff'/>\n");
            appendLightAction(builder, "➕", id, "plus");
            builder.append("<td id='main_").append(id).append("' title='").append(actor.getLddOutput().getDeviceName()).append(", max ")
                    .append(currentValueFormatter.format(actor.getMaxOutputCurrent())).append(" A' class='main'>")
                    .append("<div id='lt_").append(id).append("' class='lightTitle'></div><div id='pd_").append(id).append("' class='powerDetail'></div>\n");
            appendLightAction(builder, "➖", id, "minus");
            builder.append('\n');
        }
        builder.append("</table>");
    }

    private void appendLightAction(StringBuilder builder, String label, String id, String action) {
        appendClickableRow(builder, id, action, "lightAction").append(label).append("\n");
    }

    private StringBuilder appendClickableRow(StringBuilder builder, String id, String action, String clazz) {
        return builder.append("<td id='act_").append(id).append('_').append(action).append("' onClick=\"status.doAction('").append(id).append("', '").append(action)
                .append("')\" class='").append(clazz).append("'>");
    }
}
