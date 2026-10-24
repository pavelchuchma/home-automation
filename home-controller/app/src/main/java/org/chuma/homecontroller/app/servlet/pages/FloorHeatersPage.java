package org.chuma.homecontroller.app.servlet.pages;

import java.util.List;
import java.util.Map;

import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class FloorHeatersPage extends AbstractPage {
    private static final int ITEMS_PER_ROW = 4;
    final List<FloorHeater> floorHeaters;

    public FloorHeatersPage(List<FloorHeater> floorHeaters, Iterable<Page> links) {
        super("/floorHeaters", "Podlahovka", "Podlahovka", "favicon.png", links);
        this.floorHeaters = floorHeaters;
    }

    @Override
    public String[] getStylesheets() {
        return new String[]{"commons.css", "floorHeaters.css"};
    }

    @Override
    public String[] getScripts() {
        return new String[]{
                "commons.js",
                "status.js",
                "items/baseItem.js",
                "items/onOffItem.js",
                "items/floorHeaterItem.js",
                GetBackendUrlJs.PATH,
                VIRTUAL_CONFIGURATION_JS_FILENAME,
                "floorHeaters.js"
        };
    }

    @Override
    public void appendContent(StringBuilder builder, Map<String, String[]> requestParameters) {
        for (int i = 0; i < floorHeaters.size(); i += ITEMS_PER_ROW) {
            appendRow(builder, floorHeaters.subList(i, Math.min(i + ITEMS_PER_ROW, floorHeaters.size())));
        }
    }

    private void appendRow(StringBuilder builder, List<FloorHeater> row) {
        builder.append("<br/><br/><table class='buttonTable'><tr>");
        for (FloorHeater fh : row) {
            String id = fh.actor().getId();
            builder.append("<td id='").append(id).append("' class='floorHeaterItem' onClick=\"status.doAction('").append(id).append("', 'toggle')\">\n")
                    .append("<div id='act_").append(id).append("' class='floorHeaterState'>?</div>\n")
                    .append("<div class='floorHeaterName'>").append(fh.room()).append("</div>\n")
                    .append("<div class='floorHeaterPower'>").append(fh.powerW()).append(" W</div>\n");
        }
        // keep the cells of a shorter last row as wide as the full rows above
        builder.append("<td class='floorHeaterEmpty'>".repeat(ITEMS_PER_ROW - row.size()));
        builder.append("</table>");
    }

    /**
     * @param room   room name shown on the page
     * @param powerW installed heating cable power
     */
    public record FloorHeater(String room, int powerW, IOnOffActor actor) {
    }
}
