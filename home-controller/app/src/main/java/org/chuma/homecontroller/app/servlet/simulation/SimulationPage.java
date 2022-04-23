package org.chuma.homecontroller.app.servlet.simulation;

import java.util.Arrays;
import java.util.Map;

import org.chuma.homecontroller.app.servlet.pages.AbstractPage;
import org.chuma.homecontroller.app.servlet.pages.Page;
import org.chuma.homecontroller.base.node.ConnectedDevice;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.Pin;
import org.chuma.homecontroller.base.packet.simulation.SimulatedNode;
import org.chuma.homecontroller.base.packet.simulation.SimulatedPacketUartIO;
import org.chuma.homecontroller.controller.device.AbstractConnectedDevice;

public class SimulationPage extends AbstractPage {
    private SimulatedPacketUartIO simulator;

    public SimulationPage(Iterable<Page> links, SimulatedPacketUartIO simulator) {
        super("/simulation", "System Simulation", "Simulation", "favicon.png", links);
        this.simulator = simulator;
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
        String[] result = Arrays.copyOf(orig, orig.length + 2);
        result[orig.length] = "websocket.js";
        result[orig.length + 1] = "simulation.js";
        return result;
    }

    @Override
    protected void appendContent(StringBuilder sb, Map<String, String[]> requestParameters) {
        for (SimulatedNode simNode : simulator.getSimulatedNodes()) {
            int id = simNode.getId();
            Node node = simNode.getNode();
            sb.append("<div>\n");
            sb.append("<h1 onclick=\"showHide('node.").append(id).append("');\">Node #").append(id);
            if (node != null) {
                sb.append(": ").append(node.getName());
            }
            sb.append("</h1>\n");
            sb.append("<table class=\"node-table\" id=\"node.").append(id).append("\"><tr>\n");
            // PIC view
            sb.append("<td rowspan=\"2\">\n");
            generatePic(sb, simNode);
            sb.append("</td>\n");
            // Devices
            generateConnector(sb, simNode, 1);
            generateConnector(sb, simNode, 2);
            generateConnector(sb, simNode, 3);
            sb.append("</tr><tr><td colspan=\"3\">\n");
            sb.append("<ul id=\"msg.").append(id).append("\"></ul>");
            sb.append("</td></tr></table>\n");
            sb.append("</div>\n");
        }
    }
    
    private void generatePic(StringBuilder sb, SimulatedNode simNode) {
        sb.append("<table class=\"dip\">\n");
        ChipGenerator g = new ChipGenerator(sb, simNode, "pic");
        g.addPin(-1, 1, "MCLR"); g.addPin(Pin.pinB7, "B7/un");
        g.addPin(Pin.pinA0); g.addPin(Pin.pinB6, "B6/un");
        g.addPin(Pin.pinA1, "A1/un"); g.addPin(Pin.pinB5);
        g.addPin(Pin.pinA2); g.addPin(Pin.pinB4);
        g.addPin(Pin.pinA3); g.addPin(Pin.pinB3, "B3/CANRX");
        g.addPin(Pin.pinA4, "A4/un"); g.addPin(Pin.pinB2, "B2/CANTX");
        g.addPin(Pin.pinA5); g.addPin(Pin.pinB1);
        g.addPin(-1, 1, "Vss"); g.addPin(Pin.pinB0);
        g.addPin(Pin.pinA7); g.addPin(-1, 1, "Vdd");
        g.addPin(Pin.pinA6); g.addPin(-1, 1, "Vss");
        g.addPin(Pin.pinC0); g.addPin(Pin.pinC7);
        g.addPin(Pin.pinC1); g.addPin(Pin.pinC6);
        g.addPin(Pin.pinC2); g.addPin(Pin.pinC5);
        g.lastRow();
        g.addPin(Pin.pinC3); g.addPin(Pin.pinC4);
        sb.append("</table>\n");
    }
    

    private void generateConnector(StringBuilder sb, SimulatedNode simNode, int connectorId) {
        sb.append("<td>");
        Node node = simNode.getNode();
        if (node != null) {
            for (ConnectedDevice d : node.getDevices()) {
                if (d.getConnectorNumber() == connectorId && d instanceof AbstractConnectedDevice) {
                    AbstractConnectedDevice device = (AbstractConnectedDevice)d;
                    Pin[] layout = AbstractConnectedDevice.layout[connectorId - 1];
                    sb.append("<table class=\"dip\">\n");
                    sb.append("<tr><td colspan=\"6\" class=\"title\">Conn #").append(connectorId).append("</td></tr>\n");
                    sb.append("<tr><td colspan=\"6\" class=\"title\">");
                    if (device.getId() != null) {
                        sb.append(device.getId());
                    }
                    sb.append("</td></tr>\n");
                    ChipGenerator g = new ChipGenerator(sb, simNode, "conn");
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

    private static class ChipGenerator {
        private StringBuilder sb;
        private SimulatedNode node;
        private String idPrefix;
        // 0 top, 1 middle, 2 bottom
        private int rowState;
        private boolean left;

        public ChipGenerator(StringBuilder sb, SimulatedNode node, String idPrefix) {
            this.sb = sb;
            this.node = node;
            this.idPrefix = idPrefix;
            left = true;
        }

        public void addPin(Pin[] layout, int pin) {
            String name = layout[pin].getPort() + Integer.toString(layout[pin].getPinIndex());
            addPin(layout[pin], left ? pin + "&nbsp;/&nbsp;" + name : name + "&nbsp;/&nbsp;" + pin);
        }

        public void addPin(Pin pin) {
            addPin(pin, null);
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
            String value;
            if (port < 0) {
                // Reserved
                pinId = null;
                out = pin == 0 ? true : false;
                value = "?";
            } else {
                // Valid pin
                pinId = (char)(port + 'A') + Integer.toString(pin);
                int tris = (node.readRam(SimulatedPacketUartIO.TRIS_ADDRESS[port]) >> pin) & 1;
                if (tris == 0) {
                    // Output
                    out = true;
                    int pwm = node.getManualPwm(port, pin);
                    if (pwm < 0) {
                        // Normal output
                        value = Integer.toString((node.readRam(SimulatedPacketUartIO.PORT_ADDRESS[port]) >> pin) & 1);
                    } else {
                        // PWM
                        value = Integer.toString((int)(pwm * 100 / 48)) + "%";
                    }
                } else {
                    // Input
                    out = false;
                    value = Integer.toString((node.readRam(SimulatedPacketUartIO.PORT_ADDRESS[port]) >> pin) & 1);
                }
            }
            if (left) {
                // Write left pin - also start row
                sb.append("<tr class=\"").append(rowState == 0 ? "top" : rowState == 2 ? "bottom" : "").append("\">");
                value(pinId, value);
                direction(pinId, out ? "out": "in");
                pinName(name);
                left = false;
            } else {
                // Write right pin - also close row
                pinName(name);
                direction(pinId, out ? "out" : "in");
                value(pinId, value);
                sb.append("</tr>\n");
                left = true;
            }
            rowState = 1;
        }

        public void lastRow() {
            rowState = 2;
        }

        private void value(String pinId, String value) {
            sb.append("<td class=\"value\"");
            if (pinId != null) {
                sb.append(" id=\"").append(idPrefix).append(".").append(node.getId()).append(".").append(pinId).append(".value\" onclick=\"javascript:clicked('").append(node.getId()).append("', '").append(pinId).append("');\"");
            }
            sb.append(">");
            sb.append(value);
            sb.append("</td>");
        }

        private void direction(String pinId, String dir) {
            sb.append("<td class=\"dir ").append(left ? "left " : "right ").append(dir).append("\"");
            if (pinId != null) {
                sb.append(" id=\"").append(idPrefix).append(".").append(node.getId()).append(".").append(pinId).append(".dir\"");
            }
            sb.append("><span></td>");
        }

        private void pinName(String name) {
            sb.append("<td class=\"name ").append(left ? "left" : "right").append("\">").append(name).append("</td>");  
        }
    }
}
