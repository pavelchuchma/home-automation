package org.chuma.homecontroller.app.servlet.pages;

import java.util.List;
import java.util.Map;

import org.chuma.homecontroller.extensions.external.floorheater.FloorHeaterManager.Room;

public class FloorHeatersPage extends AbstractPage {
    private static final int ITEMS_PER_ROW = 4;
    final List<Room> rooms;

    public FloorHeatersPage(List<Room> rooms, Iterable<Page> links) {
        super("/floorHeaters", "Podlahovka", "Podlahovka", "favicon.png", links);
        this.rooms = rooms;
    }

    @Override
    public String[] getStylesheets() {
        return new String[]{"commons.css", "settingsSheet.css", "floorHeaters.css"};
    }

    @Override
    public String[] getScripts() {
        return new String[]{
                "commons.js",
                "status.js",
                "settingsSheet.js",
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
        for (int i = 0; i < rooms.size(); i += ITEMS_PER_ROW) {
            appendRow(builder, rooms.subList(i, Math.min(i + ITEMS_PER_ROW, rooms.size())));
        }
    }

    private void appendRow(StringBuilder builder, List<Room> row) {
        builder.append("<br/><br/><table class='buttonTable'><tr>");
        for (Room fh : row) {
            String id = fh.id();
            builder.append("<td id='").append(id).append("' class='floorHeaterItem'>\n")
                    .append("<div id='act_").append(id).append("' class='floorHeaterState'>?</div>\n")
                    .append("<div class='floorHeaterName'>").append(fh.name()).append("</div>\n")
                    .append("<div id='sch_").append(id).append("' class='floorHeaterSchedule'></div>\n")
                    .append("<div class='floorHeaterPower'>").append(fh.powerW()).append(" W</div>\n");
        }
        // keep the cells of a shorter last row as wide as the full rows above
        builder.append("<td class='floorHeaterEmpty'>".repeat(ITEMS_PER_ROW - row.size()));
        builder.append("</table>");
    }
}
