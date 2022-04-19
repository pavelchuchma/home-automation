package org.chuma.homecontroller.app.servlet.pages;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.chuma.homecontroller.app.configurator.Options;
import org.chuma.homecontroller.app.servlet.ServletAction;

/**
 * Page for changing "options". 
 */
public class OptionsPage extends AbstractPage {
    private Options options;

    public OptionsPage(Options options, Iterable<Page> links, Collection<ServletAction> servletActions) {
        super("/options", "Options", "Options", "favicon.png", links);
        this.options = options;
    }

    @Override
    protected void appendContent(StringBuilder sb, Map<String, String[]> requestParameters) {
        if (!requestParameters.isEmpty()) {
            for (Entry<String, String[]> e : requestParameters.entrySet()) {
                options.put(e.getKey(), e.getValue()[0]);
            }
            try {
                options.save();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        sb.append("<br>");
        sb.append("<form action=\"").append(getPath()).append("\">\n<table>\n");
        String[] names = options.getNames().toArray(new String[0]);
        Arrays.sort(names);
        for (String n : names) {
            String v = options.get(n);
            String c = options.getComment(n);
            sb.append("<tr><td><label for=\"").append(n).append("\">").append(n).append(":</label></td>");
            sb.append("<td><input type=\"text\" id=\"").append(n).append("\" name=\"").append(n).append("\" value=\"").append(v == null ? "" : v).append("\"></td>");
            sb.append("<td>").append(c != null ? c : "").append("</td></tr>\n");
        }
        sb.append("<tr><td><input type=\"submit\" value=\"Save\"></td colspan=\"2\"><td></td></tr>");
        sb.append("</table></form>");
    }
}
