package org.chuma.homecontroller.app.servlet.pages;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.chuma.homecontroller.base.node.ConnectedDevice;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.Pin;
import org.chuma.homecontroller.controller.device.AbstractConnectedDevice;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfo;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoRegistry;

public class GenericControlPage extends AbstractPage {
    private NodeInfoRegistry nodeInfoRegistry;
    
    public GenericControlPage(NodeInfoRegistry nodeInfoRegistry, Iterable<Page> links) {
        super("/generic-control", "Generic Control", "GenCtrl", "favicon.png", links);
        this.nodeInfoRegistry = nodeInfoRegistry;
    }

    @Override
    public String[] getStylesheets() {
        String[] orig = super.getStylesheets();
        String[] result = Arrays.copyOf(orig, orig.length + 1);
        result[orig.length] = "simulation.css";
        return result;
    }
    
    @Override
    public String[] getScripts() {
        String[] orig = super.getScripts();
        String[] result = Arrays.copyOf(orig, orig.length + 1);
        result[orig.length] = "generic-control.js";
        return result;
    }

    @Override
    protected void appendContent(StringBuilder sb, Map<String, String[]> requestParameters) {
        for (Iterator<NodeInfo> it = nodeInfoRegistry.getNodeInfos().iterator(); it.hasNext();) {
            NodeInfo nodeInfo = it.next();
            Node node = nodeInfo.getNode();
            int id = node.getNodeId();
            sb.append("<div>\n");
            sb.append("<h1 onclick=\"showHide('node.").append(id).append("');\">Node #").append(id);
            if (node != null) {
                sb.append(": ").append(node.getName());
            }
            sb.append("</h1>\n");
            sb.append("<table id=\"node.").append(id).append("\"><tr>\n");
            // Devices
            generateConnector(sb, node, 1);
            generateConnector(sb, node, 2);
            generateConnector(sb, node, 3);
            sb.append("</tr></table>\n");
            sb.append("</div>\n");
        }
    }

    // Very similar to SimulationPage.generateConnector()
    private void generateConnector(StringBuilder sb, Node node, int connectorId) {
        sb.append("<td>");
        if (node != null) {
            for (ConnectedDevice d : node.getDevices()) {
                if (d.getConnectorNumber() == connectorId && d instanceof AbstractConnectedDevice) {
                    AbstractConnectedDevice device = (AbstractConnectedDevice)d;
                    Pin[] layout = AbstractConnectedDevice.layout[connectorId - 1];
                    sb.append("<table>\n");
                    sb.append("<tr><td colspan=\"6\" class=\"title\">Conn #").append(connectorId).append("</td></tr>\n");
                    sb.append("<tr><td colspan=\"6\" class=\"title\">");
                    if (device.getId() != null) {
                        sb.append(device.getId());
                    }
                    sb.append("</td></tr>\n");
                    ChipGenerator g = new ChipGenerator(sb, node, "conn");
                    g.addPin(layout, 5); g.addPin(layout, 4);
                    g.addPin(layout, 3); g.addPin(layout, 2);
                    g.addPin(layout, 1); g.addPin(layout, 0);
                    g.lastRow();
                    g.addPin(-1, 0, "GND"); g.addPin(-1, 0, "+5V");
                    sb.append("</table>\n");
                }
            }
        }
        sb.append("</td>");
    }

    // Very similar to SimulationPage.ChipGenerator
    private static class ChipGenerator {
        private StringBuilder sb;
        private Node node;
        private int outputMask;
        private String idPrefix;
        // 0 top, 1 middle, 2 bottom
        private int rowState;
        private boolean left;

        public ChipGenerator(StringBuilder sb, Node node, String idPrefix) {
            this.sb = sb;
            this.node = node;
            this.idPrefix = idPrefix;
            left = true;
            // Output mask for whole node
            outputMask = 0;
            for (ConnectedDevice device : node.getDevices()) {
                outputMask |= device.getOutputMasks();
            }
        }

        public void addPin(Pin[] layout, int pin) {
            String name = layout[pin].getPort() + Integer.toString(layout[pin].getPinIndex());
            addPin(layout[pin], left ? pin + "&nbsp;/&nbsp;" + name : name + "&nbsp;/&nbsp;" + pin);
        }

        public void addPin(Pin pin, String name) {
            addPin(pin.getPortIndex(), pin.getPinIndex(), name);
        }

        public void addPin(int port, int pin, String name) {
            if (name == null && port >= 0) {
                name = (char)(port + 'A') + Integer.toString(pin);
            }
            String pinId;
            boolean out;
            if (port < 0) {
                // Reserved
                pinId = null;
                out = pin == 0 ? true : false;
            } else {
                // Valid pin
                pinId = (char)(port + 'A') + Integer.toString(pin);
                int tris = outputMask & (1 << (8 * port + pin));
                if (tris != 0) {
                    // Output
                    out = true;
                } else {
                    // Input
                    out = false;
                }
            }
            if (left) {
                // Write left pin - also start row
                sb.append("<tr class=\"").append(rowState == 0 ? "top" : rowState == 2 ? "bottom" : "").append("\">");
                value(pinId, out);
                direction(pinId, out ? "out": "in");
                pinName(name);
                left = false;
            } else {
                // Write right pin - also close row
                pinName(name);
                direction(pinId, out ? "out" : "in");
                value(pinId, out);
                sb.append("</tr>\n");
                left = true;
            }
            rowState = 1;
        }

        public void lastRow() {
            rowState = 2;
        }

        private void value(String pinId, boolean out) {
            pwm(pinId, false, out);
            sb.append("<td class=\"value\"");
            if (pinId != null) {
                //sb.append(" id=\"").append(idPrefix).append(".").append(node.getNodeId()).append(".").append(pinId).append(".value\" onclick=\"javascript:clicked('").append(node.getNodeId()).append("', '").append(pinId).append("');\"");
                sb.append(" id=\"").append(idPrefix).append(".").append(node.getNodeId()).append(".").append(pinId).append(".value\"");
                if (out) {
                    sb.append(" onclick=\"javascript:clicked('").append(node.getNodeId()).append("', '").append(pinId).append("');\"");
                }
            }
            sb.append(">?</td>");
            pwm(pinId, true, out);
        }

        private void pwm(String pinId, boolean plus, boolean out) {
            sb.append("<td class=\"pwm\"");
            if (out) {
                sb.append(" onclick=\"javascript:clickedPwm('").append(node.getNodeId()).append("', '").append(pinId).append("', ").append(plus).append(");\"");
            }
            sb.append(">");
            if (out) {
                sb.append(plus ? "+" : "-");
            }
            sb.append("</td>");
        }

        private void direction(String pinId, String dir) {
            sb.append("<td class=\"dir ").append(left ? "left " : "right ").append(dir).append("\"");
            if (pinId != null) {
                sb.append(" id=\"").append(idPrefix).append(".").append(node.getNodeId()).append(".").append(pinId).append(".dir\"");
            }
            sb.append("><span></td>");
        }

        private void pinName(String name) {
            sb.append("<td class=\"name ").append(left ? "left" : "right").append("\">").append(name).append("</td>");  
        }
    }
}
