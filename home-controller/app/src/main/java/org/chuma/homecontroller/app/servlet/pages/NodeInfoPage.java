package org.chuma.homecontroller.app.servlet.pages;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jetty.server.Request;

import org.chuma.homecontroller.app.servlet.ServletAction;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfo;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoRegistry;

public class NodeInfoPage extends AbstractPage {
    final List<ServletAction> rootActions;
    final NodeInfoRegistry nodeInfoRegistry;

    public NodeInfoPage(NodeInfoRegistry nodeInfoRegistry, Iterable<Page> links, Collection<ServletAction> rootActions) {
        super("/nodes", "Node Info", "Nodes", "favicon.png", links);
        this.nodeInfoRegistry = nodeInfoRegistry;
        this.rootActions = new ArrayList<>(rootActions);
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
    public void appendContent(StringBuilder builder) {
        int i = 1;
        boolean rootActionAdded = false;
        for (ServletAction action : rootActions) {
            builder.append("<a href='").append(getPath()).append("/a").append(i++).append("'>")
                    .append(action.name).append("</a>&nbsp;&nbsp;&nbsp;&nbsp;");
            rootActionAdded = true;
        }
        if (rootActionAdded) {
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

    @Override
    public void handle(String target, Request request, HttpServletResponse response) throws IOException {
        int actionIndex = tryTargetMatchAndParseArg(target, getPath() + "/a");
        if (actionIndex > 0 && actionIndex <= rootActions.size()) {
            rootActions.get(actionIndex - 1).action.perform(-1);
        }
        super.handle(target, request, response);
    }
}
