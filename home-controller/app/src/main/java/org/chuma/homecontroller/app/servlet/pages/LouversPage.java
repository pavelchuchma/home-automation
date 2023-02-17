package org.chuma.homecontroller.app.servlet.pages;

import java.util.List;
import java.util.Map;

import org.chuma.homecontroller.controller.controller.LouversController;

public class LouversPage extends AbstractPage {
    final List<LouversController> louversControllers;

    public LouversPage(List<LouversController> louversControllers, Iterable<Page> links) {
        super("/louvers", "Žaluzie", "Žaluzie", "favicon.png", links);
        this.louversControllers = louversControllers;
    }

    @Override
    public String[] getStylesheets() {
        return new String[]{"commons.css", "louvers.css"};
    }

    @Override
    public String[] getScripts() {
        return new String[]{
                "commons.js",
                "status.js",
                "items/baseItem.js",
                "items/louversItem.js",
                GetBackendUrlJs.PATH,
                VIRTUAL_CONFIGURATION_JS_FILENAME,
                "louvers.js"
        };
    }

    @Override
    public void appendContent(StringBuilder builder, Map<String, String[]> requestParameters) {
        for (int i = 0; i < louversControllers.size(); i += 4) {
            int count = (i + 8 < louversControllers.size()) ? 4 : louversControllers.size() - i;
            builder.append(getLouversTable(i, count));
            if (count > 4) {
                break;
            }
        }
    }

    private String getLouversTable(int startIndex, int count) {
        StringBuilder builder = new StringBuilder();
        builder.append("<br/><br/><table class='buttonTable'>");
        for (int i = startIndex; i < startIndex + count; i++) {
            LouversController lc = louversControllers.get(i);

            builder.append("<td id='").append(lc.getId()).append("' class='louversItem'>\n" +
                    "<table>\n");

            appendLouversIcon(builder, "?", lc.getId(), "up");
            appendLouversIcon(builder, "☀", lc.getId(), "outshine");
            appendLouversIcon(builder, "?", lc.getId(), "blind");

            builder.append("<tr><td colspan='3' class='louversName'>").append(lc.getLabel()).append("<tr>\n</table>\n");
        }
        builder.append("</table>");
        return builder.toString();
    }

    private void appendLouversIcon(StringBuilder builder, String icon, String id, String action) {
        builder.append("<td id='act_").append(id).append('_').append(action).append("' onClick=\"status.doAction('").append(id).append("', '").append(action)
                .append("')\" class='louversArrow'>").append(icon).append("\n");
    }
}
