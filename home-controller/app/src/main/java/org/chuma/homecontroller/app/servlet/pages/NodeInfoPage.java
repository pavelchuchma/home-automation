package org.chuma.homecontroller.app.servlet.pages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.chuma.homecontroller.app.servlet.ServletAction;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfo;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoRegistry;

public class NodeInfoPage extends AbstractPage {
    final List<ServletAction> servletActions;
    final NodeInfoRegistry nodeInfoRegistry;

    public NodeInfoPage(NodeInfoRegistry nodeInfoRegistry, Iterable<Page> links, Collection<ServletAction> servletActions) {
        super("/nodes", "Node Info", "Nodes", "favicon.png", links);
        this.nodeInfoRegistry = nodeInfoRegistry;
        this.servletActions = new ArrayList<>(servletActions);
    }

    @Override
    public String[] getStylesheets() {
        return new String[]{"commons.css", "nodeInfo.css"};
    }

    @Override
    public String[] getScripts() {
        return new String[]{
                "commons.js",
                "status.js",
                "items/baseItem.js",
                "items/nodeInfoItem.js",
                VIRTUAL_CONFIGURATION_JS_FILENAME,
                "nodeInfo.js"
        };
    }

    @Override
    public void appendContent(StringBuilder builder, Map<String, String[]> requestParameters) {
        for (ServletAction action : servletActions) {
            builder.append("<button onClick=\"performServletAction('").append(action.getId()).append("')\">")
                    .append(action.getLabel()).append("</button>&nbsp;&nbsp;&nbsp;&nbsp;");
        }
        if (!servletActions.isEmpty()) {
            builder.append("<br/>");
        }

        builder.append("<table class='nodeTable'>\n" +
                "<tr><th class=''>Node #<th class=''>Ping Age<th class=''>Boot Time<th class=''>Build Time<th class=''>MessageLog");

        for (NodeInfo info : nodeInfoRegistry.getNodeInfos()) {
            int id = info.getNode().getNodeId();
            builder.append("<tr><td id='name").append(id).append("'><td id='pa").append(id).append("'><td id='boot")
                    .append(id).append("'><td id='bld").append(id).append("'><td id='msg").append(id).append("' class='messageLog'>\n");
        }

        builder.append("</table>");
    }
}
