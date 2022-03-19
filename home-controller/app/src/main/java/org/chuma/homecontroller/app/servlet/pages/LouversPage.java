package org.chuma.homecontroller.app.servlet.pages;

import org.chuma.homecontroller.controller.controller.LouversController;

public class LouversPage extends AbstractPage {
    final LouversController[] louversControllers;

    public LouversPage(LouversController[] louversControllers, Iterable<Page> links) {
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
                "configuration-pi.js",
                "louvers.js"
        };
    }

    @Override
    public void appendContent(StringBuilder builder) {
        for (int i = 0; i < louversControllers.length; i += 4) {
            int count = (i + 8 < louversControllers.length) ? 4 : louversControllers.length - i;
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
            LouversController lc = louversControllers[i];

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
