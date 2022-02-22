package org.chuma.homecontroller.app.servlet.pages;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.eclipse.jetty.server.Request;

import org.chuma.homecontroller.app.servlet.Handler;
import org.chuma.homecontroller.app.servlet.ServletAction;
import org.chuma.homecontroller.base.node.MessageType;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.controller.nodeinfo.LogMessage;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfo;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoRegistry;

public class NodeInfoPage extends AbstractPage {
    private static final String FLOOR_PLAN_LOCATION = "/floorPlan.html";
    final Iterable<Page> pages;
    final List<ServletAction> rootActions;
    final NodeInfoRegistry nodeInfoRegistry;

    public NodeInfoPage(NodeInfoRegistry nodeInfoRegistry, Iterable<Handler> handlers, Collection<ServletAction> rootActions) {
        super("/", "Node Info", "Nodes", "favicon.png");
        this.nodeInfoRegistry = nodeInfoRegistry;
        this.rootActions = new ArrayList<>(rootActions);

        List<Page> pages = new ArrayList<>();
        for (Handler h : handlers) {
            if (h instanceof Page) {
                pages.add((Page) h);
            }
        }
        this.pages = pages;
    }

    @Override
    int getRefreshInterval() {
        return 1;
    }

    @Override
    public void appendContent(StringBuilder builder) {
        int i = 1;
        boolean rootActionAdded = false;
        for (ServletAction action : rootActions) {
            builder.append(String.format("<a href='/a%d'>%s</a>&nbsp;&nbsp;&nbsp;&nbsp;", i++, action.name));
            rootActionAdded = true;
        }
        if (rootActionAdded) {
            builder.append("<br/>");
        }

        for (Page page : pages) {
            builder.append("<a href='").append(page.getRootPath()).append("'>")
                    .append(page.getReferenceName()).append("...</a>&nbsp;&nbsp;&nbsp;&nbsp;");
        }

        builder.append("<a href='" + FLOOR_PLAN_LOCATION + "'>Obyvak...</a>&nbsp;&nbsp;&nbsp;&nbsp;");

        builder.append("<table class='nodeTable'>\n" +
                "<tr><th class=''>Node #<th class=''>Last Ping Time<th class=''>Boot Time<th class=''>Build Time<th class=''>MessageLog");

        for (NodeInfo info : nodeInfoRegistry.getNodeInfos()) {
            if (info != null) {
                String lastPingClass = "errorValue";
                String lastPingString = "-";
                long lastPing;
                if (info.getLastPingTime() != null) {
                    lastPing = (new Date().getTime() - info.getLastPingTime().getTime()) / 1000;
                    if (lastPing <= Node.HEART_BEAT_PERIOD) lastPingClass = "fineValue";
                    lastPingString = lastPing + " s";
                }
                builder.append(String.format("<tr><td>%d-%s<td class='%s'>%s<td>%s<td>%s<td class='messageLog'>",
                        info.getNode().getNodeId(), info.getNode().getName(), lastPingClass, lastPingString,
                        info.getBootTime(), info.getBuildTime()));

                for (LogMessage m : info.getMessageLog()) {
                    builder.append(String.format("<div class='%s'>%s%s</div>",
                            (m.received) ? "receivedMessage" : "sentMessage",
                            MessageType.toString(m.packet.messageType),
                            (m.packet.data != null) ? Arrays.toString(m.packet.data) : ""));
                }
                builder.append("\n");
            }
        }
        builder.append("</table>");
    }

    @Override
    public void handle(String target, Request request, HttpServletResponse response) throws IOException {
        int actionIndex = tryTargetMatchAndParseArg(target, "/a");
        if (actionIndex > 0 && actionIndex <= rootActions.size()) {
            rootActions.get(actionIndex - 1).action.perform(-1);
        }
        super.handle(target, request, response);
    }
}
