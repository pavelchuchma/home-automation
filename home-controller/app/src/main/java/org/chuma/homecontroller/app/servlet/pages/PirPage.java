package org.chuma.homecontroller.app.servlet.pages;

import java.util.List;
import java.util.Map;

import org.chuma.homecontroller.controller.PirStatus;

public class PirPage extends AbstractPage {
    final List<PirStatus> pirStatusList;

    public PirPage(List<PirStatus> pirStatusList, Iterable<Page> links) {
        super("/sensors", "Senzory", "Senzory", "favicon.png", links);
        this.pirStatusList = pirStatusList;
    }

    @Override
    public String[] getStylesheets() {
        return new String[]{"commons.css", "sensors.css"};
    }

    @Override
    public String[] getScripts() {
        return new String[]{
                "commons.js",
                "status.js",
                "items/baseItem.js",
                "items/pirItem.js",
                VIRTUAL_CONFIGURATION_JS_FILENAME,
                "sensors.js"
        };
    }

    @Override
    public void appendContent(StringBuilder builder, Map<String, String[]> requestParameters) {
        builder.append("<br/><br/><table class='sensorTable'>");

        for (PirStatus status : pirStatusList) {
            final String id = status.getId();
            builder.append("<tr>");
            builder.append("<td id='act_").append(id);
            builder.append("'><td class='name'>").append(status.getName());
            builder.append("<td id='dt_").append(id).append("' class='pirDate'>");
        }
        builder.append("</table>");
    }
}
