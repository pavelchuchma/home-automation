package org.chuma.homecontroller.app.servlet.pages;

import java.util.Map;

import org.apache.commons.lang3.Validate;

import org.chuma.homecontroller.app.servlet.rest.AbstractRestHandler;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfo;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoRegistry;

public class NodeInfoDetailPage extends AbstractPage {
    final NodeInfoRegistry nodeInfoRegistry;

    public NodeInfoDetailPage(NodeInfoRegistry nodeInfoRegistry) {
        super("/nodes/detail", "Node Detail", "Node Detail", "favicon.png", null);
        this.nodeInfoRegistry = nodeInfoRegistry;
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
        };
    }

    @Override
    public void appendContent(StringBuilder builder, Map<String, String[]> requestParameters) {
        int id = AbstractRestHandler.getMandatoryIntParam(requestParameters, "id");
        NodeInfo nodeInfo = nodeInfoRegistry.getNodeInfo(id);
        Validate.notNull(nodeInfo, "no item with id '" + id + "' found");

//        builder.append("<table class='nodeTable'>\n" +
//                "<tr><th class=''>Node #<th class=''>Ping Age<th class=''>Boot Time<th class=''>Build Time<th class=''>MessageLog");

        builder.append(nodeInfo.getNode().getName());

//        builder.append("</table>");
    }
}
