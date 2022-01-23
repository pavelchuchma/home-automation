package org.chuma.homecontroller.app.servlet.pages;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import org.eclipse.jetty.server.Request;

import org.chuma.homecontroller.controller.controller.Activity;
import org.chuma.homecontroller.controller.controller.LouversController;

public class LouversPage extends AbstractPage {
    public static final String CLASS_LOUVERS_ARROW = "louversArrow";
    public static final String CLASS_LOUVERS_ARROW_ACTIVE = "louversArrow louversArrow-moving";
    public static final String TARGET_LOUVERS = "/louvers";
    final LouversController[] louversControllers;
    final Map<String, LouversController> louversControllerMap;

    public LouversPage(LouversController[] louversControllers, Map<String, LouversController> louversControllerMap) {
        super(TARGET_LOUVERS, "Žaluzie", "Žaluzie", "favicon.png");
        this.louversControllers = louversControllers;
        this.louversControllerMap = louversControllerMap;
    }

    public String getBody() {
        StringBuilder builder = new StringBuilder();

        builder.append("<html>")
                .append(getHtmlHead())
                .append("<body><a href='")
                .append(getRootPath()).append("'>Refresh</a>&nbsp;&nbsp;&nbsp;&nbsp;<a href='/'>Back</a>\n");

        for (int i = 0; i < louversControllers.length; i += 4) {
            int count = (i + 8 < louversControllers.length) ? 4 : louversControllers.length - i;
            builder.append(getLouversTable(i, count));
            if (count > 4) {
                break;
            }
        }

        builder.append("</body></html>");
        return builder.toString();
    }

    private String getLouversTable(int startIndex, int count) {
        StringBuilder builder = new StringBuilder();
        builder.append("<br/><br/><table class='buttonTable'>");
        for (int i = startIndex; i < startIndex + count; i++) {
            LouversController lc = louversControllers[i];
            String upArrow = (lc.isUp()) ? "▲" : "△";
            String downArrow = (lc.isDown()) ? "▼" : "▽";
            String outshineCharacter = "☀";

            Activity activity = lc.getActivity();
            String upArrowClazz = (activity == Activity.movingUp) ? CLASS_LOUVERS_ARROW_ACTIVE : CLASS_LOUVERS_ARROW;
            String downArrowClazz = (activity == Activity.movingDown) ? CLASS_LOUVERS_ARROW_ACTIVE : CLASS_LOUVERS_ARROW;
            builder.append("    <td class='louversItem'>\n" +
                    "        <table>\n");

            appendLouversIcon(builder, upArrow, 3 * i, upArrowClazz);
            appendLouversIcon(builder, outshineCharacter, 3 * i + 1, CLASS_LOUVERS_ARROW);
            appendLouversIcon(builder, downArrow, 3 * i + 2, downArrowClazz);

            builder.append(String.format("<tr>" +
                    "            <td colspan=\"3\" class='louversName'>%s<tr>\n" +
                    "        </table>\n", lc.getLabel()));

        }
        builder.append("</table>");
        return builder.toString();
    }

    private void appendLouversIcon(StringBuilder builder, String icon, int linkAction, String clazz) {
        builder.append(String.format("<td onClick=\"document.location.href='%s%s'\" class='%s'>%s\n", "TODO", linkAction, clazz, icon));
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        sendOkResponse(baseRequest, response, getBody());
    }
}
