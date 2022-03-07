package org.chuma.homecontroller.app.servlet.pages;

import java.util.List;

import org.chuma.homecontroller.controller.PirStatus;

public class PirPage extends AbstractPage {
    final List<PirStatus> pirStatusList;

    public PirPage(List<PirStatus> pirStatusList) {
        super("/sensors", "Senzory", "Senzory", "favicon.png");
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
                "configuration-pi.js",
                "sensors.js"
        };
    }

    @Override
    public void appendContent(StringBuilder builder) {
        builder.append("<a href='/'>Back</a>\n");
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
