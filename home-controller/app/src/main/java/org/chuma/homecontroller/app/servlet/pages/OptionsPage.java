package org.chuma.homecontroller.app.servlet.pages;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.chuma.homecontroller.base.utils.Options;

/**
 * Page for changing "options".
 */
public class OptionsPage extends AbstractPage {
    private final Options options;

    public OptionsPage(Options options, Iterable<Page> links) {
        super("/options", "Options", "Options", "favicon.png", links);
        this.options = options;
    }

    @Override
    protected void appendContent(StringBuilder sb, Map<String, String[]> requestParameters) {
        if (!requestParameters.isEmpty()) {
            Set<String> names = new HashSet<>(options.getNames());
            for (Entry<String, String[]> e : requestParameters.entrySet()) {
                options.put(e.getKey(), e.getValue()[0]);
                names.remove(e.getKey());
            }
            // set "false" to boolean "true" options if not present in the POST request -> unchecked checkbox
            for (String name : names) {
                if ("true".equals(options.get(name))) {
                    options.put(name, false);
                }
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
            if ("true".equals(v) || "false".equals(v)) {
                sb.append("<td><input type=\"checkbox\" id=\"").append(n).append("\" name=\"").append(n).append("\" value=\"true\"");
                if ("true".equals(v)) {
                    sb.append(" checked");
                }
                sb.append("></td>");
            } else {
                sb.append("<td><input type=\"text\" id=\"").append(n).append("\" name=\"").append(n).append("\" value=\"").append(v == null ? "" : v).append("\"></td>");
            }
            sb.append("<td>").append(c != null ? c : "").append("</td></tr>\n");
        }
        sb.append("<tr><td><input type=\"submit\" value=\"Save\"></td colspan=\"2\"><td></td></tr>");
        sb.append("</table></form>");
    }
}
