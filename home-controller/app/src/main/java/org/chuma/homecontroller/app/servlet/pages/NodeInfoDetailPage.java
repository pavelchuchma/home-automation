package org.chuma.homecontroller.app.servlet.pages;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.app.servlet.rest.AbstractRestHandler;
import org.chuma.homecontroller.app.servlet.rest.impl.NodeTestRunner;
import org.chuma.homecontroller.base.node.ConnectedDevice;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.Pic;
import org.chuma.homecontroller.base.node.Pin;
import org.chuma.homecontroller.controller.device.AbstractConnectedDevice;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfo;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoRegistry;

public class NodeInfoDetailPage extends AbstractPage {
    static Logger log = LoggerFactory.getLogger(NodeTestRunner.class.getName());
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
    public void appendAdditionalHtmlHeaders(StringBuilder builder, Map<String, String[]> requestParameters) {
        int id = AbstractRestHandler.getMandatoryIntParam(requestParameters, "id");
        builder.append("    <script>window.onload = function () {onLoad('").append(id).append("');}</script>");
    }

    @Override
    public void appendContent(StringBuilder builder, Map<String, String[]> requestParameters) {
        int id = AbstractRestHandler.getMandatoryIntParam(requestParameters, "id");
        NodeInfo nodeInfo = nodeInfoRegistry.getNodeInfo(id);
        Validate.isTrue(nodeInfo != null, "no item with id '" + id + "' found");

        Node node = nodeInfo.getNode();
        builder.append("<div class=title>#").append(node.getNodeId()).append(" ").append(node.getName()).append("</div><br/>");

        if (nodeInfo.isResetSupported()) {
            appendButton(builder, "btnReset", "Reset", "resetNode('" + id + "')", false);
        }
        appendButton(builder, "btnTestCycle", "Test Cycle", "testNode('" + id + "', '" + NodeTestRunner.Mode.cycle + "')", true);
        appendButton(builder, "btnTestOn", "Test All On", "testNode('" + id + "', '" + NodeTestRunner.Mode.fullOn + "')", true);
        appendButton(builder, "btnTestOff", "Test All Off", "testNode('" + id + "', '" + NodeTestRunner.Mode.fullOff + "')", true);
        appendButton(builder, "btnEndTest", "End Test", "testNode('" + id + "', '" + NodeTestRunner.Mode.endTest + "')", true);

        appendNodeDebugInfo(builder, id);
    }

    private void appendButton(StringBuilder builder, String id, String title, String onClick, boolean hidden) {
        builder.append("<button ");
        if (hidden) {
            builder.append("hidden='true' ");
        }
        builder.append("id='").append(id).append("' onClick=\"").append(onClick).append("\">").append(title).append("</button>");
    }

    private void appendNodeDebugInfo(StringBuilder builder, int debugNodeId) {
        Node node = nodeInfoRegistry.getNode(debugNodeId);

        int[] portValues = new int[3];
        int[] trisValues = new int[3];

        try {
            portValues[0] = node.readMemory(Pic.PORTA);
            trisValues[0] = node.readMemory(Pic.TRISA);
            portValues[1] = node.readMemory(Pic.PORTB);
            trisValues[1] = node.readMemory(Pic.TRISB);
            portValues[2] = node.readMemory(Pic.PORTC);
            trisValues[2] = node.readMemory(Pic.TRISC);
//            readProgramMemory = node.readProgramMemory(0x3FFFFE);
            //deviceID = node.readMemory(Pic.DE)
        } catch (IOException e) {
            log.error("Failed to read node memory", e);
            builder.append(e);
            return;
        }

        builder.append("<table class='connectorTable'><tr>");
        for (int connId = 1; connId <= 3; connId++) {
            ConnectedDevice device = null;
            for (ConnectedDevice d : node.getDevices()) {
                if (d.getConnectorNumber() == connId) {
                    device = d;
                }
            }

            builder.append("<td>");
            appendSingleConnectorInfo(builder, portValues, trisValues, connId, device);
        }
        builder.append("</table>");

        appendPicPortInfo(builder, portValues, trisValues);
    }

    private void appendSingleConnectorInfo(StringBuilder builder, int[] portValues, int[] trisValues, int connId, ConnectedDevice device) {
        builder.append("<br/><table class='singleConnectorTable'><tr>");
        builder.append("<td class='connectorIdCell'>").append(connId);
        builder.append("<td class='connectorNameCell' colspan='3'><div class='deviceName'>").append((device != null) ? device.getId() : "")
                .append("</div><div class='deviceClass'>").append((device != null) ? device.getClass().getSimpleName() : "").append("</div>");
        for (int row = 1; row <= 2; row++) {
            if (row == 1) {
                builder.append("<tr><td class='pinCellVdd'>Vdd");
            } else {
                builder.append("<tr><td class='pinCellVss'>⏚");
            }
            for (int i = row; i < 7; i += 2) {
                appendConnPinDetail(builder, trisValues, portValues, connId, i);
            }
        }

        builder.append("</table>");
    }

    private void appendConnPinDetail(StringBuilder builder, int[] trisValues, int[] portValues, int connId, int pinId) {
        Pin pin = AbstractConnectedDevice.getPin(connId, pinId);
        builder.append(String.format("<td class='pinCell'>%s %d-%d",
                pin.toString().substring(3),
                applyBitMaskTo01(trisValues[pin.getPortIndex()], pin.getBitMask()),
                applyBitMaskTo01(portValues[pin.getPortIndex()], pin.getBitMask())));
    }

    private int applyBitMaskTo01(int values, int mask) {
        return ((values & mask) != 0) ? 1 : 0;
    }

    private void appendPicPortInfo(StringBuilder builder, int[] portValues, int[] trisValues) {
        builder.append("<br/><table class='nodeInfoTable'><tr>");
        builder.append("<tr><th>Name<th>Tris<th>Value");
        char[] portNames = new char[]{'A', 'B', 'C'};
        for (int port = 0; port < 3; port++)
            for (int bit = 0; bit < 8; bit++) {
                builder.append(String.format("<tr><td>%c%d<td>%d<td>%d", portNames[port], bit, applyBitMaskTo01(trisValues[port], 1 << bit), applyBitMaskTo01(portValues[port], 1 << bit)));
            }
        builder.append("</table>");
    }
}
