package org.chuma.homecontroller.app.servlet.pages;

import java.util.List;

import org.chuma.homecontroller.controller.PirStatus;

public class PirPage extends AbstractPage {
    final List<PirStatus> pirStatusList;

    public PirPage(List<PirStatus> pirStatusList) {
        super("/pir", "PIR Status", "PIR", "favicon.png");
        this.pirStatusList = pirStatusList;
    }

    @Override
    int getRefreshInterval() {
        return 1;
    }

    @Override
    public void appendContent(StringBuilder builder) {
        builder.append("<a href='").append(getRootPath())
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
    }
}
