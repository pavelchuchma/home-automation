package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.base.node.MessageType;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.controller.nodeinfo.LogMessage;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfo;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoRegistry;

public class NodeHandler extends AbstractRestHandler<NodeInfo> {

    public NodeHandler(NodeInfoRegistry nodeInfoRegistry) {
        super("nodes", "nodes", nodeInfoRegistry.getNodeInfos(),
                nodeInfo -> String.valueOf(nodeInfo.getNode().getNodeId()));
    }

    private static String dateAsString(Date date) {
        return (date != null) ? String.valueOf(date) : null;
    }

    @Override
    void writeJsonItemValues(JsonWriter jw, NodeInfo info, HttpServletRequest request) {
        final Node node = info.getNode();
        jw.addAttribute("name", node.getName());
        long lastPingAge = (info.getLastPingTime() != null) ? (new Date().getTime() - info.getLastPingTime().getTime()) / 1000 : -1;
        jw.addAttribute("lastPingAge", lastPingAge);
        jw.addAttribute("buildTime", dateAsString(info.getBuildTime()));
        jw.addAttribute("bootTime", dateAsString(info.getBootTime()));
        try (JsonWriter arrayWriter = jw.startArrayAttribute("messages")) {
            for (LogMessage m : info.getMessageLog()) {
                try (JsonWriter objectWriter = arrayWriter.startObject()) {
                    objectWriter.addAttribute("dir", (m.received) ? "r" : "s");
                    objectWriter.addAttribute("type", m.packet.messageType);
                    objectWriter.addAttribute("typeName", MessageType.toString(m.packet.messageType));
                    objectWriter.addAttribute("data", (m.packet.data != null) ? Arrays.toString(m.packet.data) : "");
                }
            }
        }
    }

    @Override
    void writeIdImpl(Map.Entry<String, NodeInfo> entry, JsonWriter objectWriter) {
        objectWriter.addAttribute("id", Integer.parseInt(entry.getKey()));
    }

    @Override
    void processAction(NodeInfo nodeInfo, Map<String, String[]> requestParameters) throws Exception {
        String action = getMandatoryStringParam(requestParameters, "action");
        if ("reset".equals(action)) {
            nodeInfo.getNode().reset();
            return;
        }
        throw new IllegalArgumentException("Unknown action '" + action + "'");
    }
}
