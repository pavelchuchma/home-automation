package org.chuma.homecontroller.app.servlet.pages;

import java.util.Comparator;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import org.chuma.homecontroller.app.servlet.rest.AbstractRestHandler;
import org.chuma.homecontroller.base.node.ConnectedDevice;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfo;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoRegistry;

public class NodeInfoDetailPage extends AbstractPage {
    final NodeInfoRegistry nodeInfoRegistry;

    public NodeInfoDetailPage(NodeInfoRegistry nodeInfoRegistry, Iterable<Page> links) {
        super("/nodes/detail", "Node Detail", "Node Detail", "favicon.png", links);
        this.nodeInfoRegistry = nodeInfoRegistry;
    }

    @Override
    public String[] getStylesheets() {
        return new String[]{"commons.css", "nodeInfoDetail.css"};
    }

    @Override
    public String[] getScripts() {
        return new String[]{
                "commons.js",
                "status.js",
                "items/baseItem.js",
                "items/nodeInfoItem.js",
                VIRTUAL_CONFIGURATION_JS_FILENAME,
                "nodeInfoDetail.js",
        };
    }

    @Override
    public void appendContent(StringBuilder builder, Map<String, String[]> requestParameters) {
        int id = AbstractRestHandler.getMandatoryIntParam(requestParameters, "id");
        NodeInfo nodeInfo = nodeInfoRegistry.getNodeInfo(id);
        Validate.notNull(nodeInfo, "no item with id '" + id + "' found");

        Node node = nodeInfo.getNode();
        builder.append("<div class=title>#").append(node.getNodeId()).append(" ").append(node.getName()).append("</div><br/>");

        if (nodeInfo.isResetSupported()) {
            builder.append("<button onClick=\"resetNode('").append(id).append("')\">Reset</button>");
        }
        builder.append("<div class=devicesTitle>Connected devices:</div><ul>\n");

        node.getDevices().stream().sorted(Comparator.comparing(ConnectedDevice::getConnectorNumber))
                .forEachOrdered(d -> builder.append("<li>").append(d.getConnectorNumber()).append(" - ").append(d.getId()).append("</li>\n"));
        builder.append("</ul>");
    }
}
