package org.chuma.homecontroller.app.servlet.pages;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.app.servlet.Handler;
import org.chuma.homecontroller.app.servlet.Servlet;
import org.chuma.homecontroller.app.servlet.ServletAction;
import org.chuma.homecontroller.base.node.MessageType;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.controller.nodeinfo.LogMessage;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfo;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoCollector;

public class NodeInfoPage extends AbstractPage {
    public static final String TARGET_NODE_INFO = "/";
    private static final String TARGET_LIGHTS_OBYVAK = "/lightsObyvak.html";
    private static final Logger log = LoggerFactory.getLogger(NodeInfoPage.class.getName());
    final NodeInfoCollector nodeInfoCollector;
    final Iterable<Page> pages;

    public NodeInfoPage(NodeInfoCollector nodeInfoCollector, Iterable<Handler> handlers) {
        super(TARGET_NODE_INFO, "Node Info", "Nodes", "favicon.png");
        this.nodeInfoCollector = nodeInfoCollector;
        List<Page> pages = new ArrayList<>();
        for (Handler h : handlers) {
            if (h instanceof Page) {
                pages.add((Page) h);
            }
        }
        this.pages = pages;
    }

    public String getBody() {
        StringBuilder builder = new StringBuilder();

        builder.append("<html><meta http-equiv='refresh' content='1;url=/'/>").append(getHtmlHead()).append("<body>");

        int i = 1;
        for (ServletAction action : Servlet.rootActions) {
            builder.append(String.format("<a href='/a%d'>%s</a>&nbsp;&nbsp;&nbsp;&nbsp;", i++, action.name));
        }
        if (!Servlet.rootActions.isEmpty()) {
            builder.append("<br/>");
        }

        for (Page page : pages) {
            builder.append("<a href='").append(page.getRootPath()).append("'>")
                    .append(page.getReferenceName()).append("...</a>&nbsp;&nbsp;&nbsp;&nbsp;");
        }

        builder.append("<a href='" + TARGET_LIGHTS_OBYVAK + "'>Obyvak...</a>&nbsp;&nbsp;&nbsp;&nbsp;");

        builder.append("<table class='nodeTable'>\n" +
                "<tr><th class=''>Node #<th class=''>Last Ping Time<th class=''>Boot Time<th class=''>Build Time<th class=''>MessageLog");

        for (NodeInfo info : nodeInfoCollector.getNodeInfoArray()) {
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
        builder.append("</body></html>");
        return builder.toString();

    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        int actionIndex = tryTargetMatchAndParseArg(target, "/a");
        if (actionIndex > 0 && actionIndex <= Servlet.rootActions.size()) {
            Servlet.rootActions.get(actionIndex - 1).action.perform(-1);
        }
        sendOkResponse(baseRequest, response, getBody());
    }
}