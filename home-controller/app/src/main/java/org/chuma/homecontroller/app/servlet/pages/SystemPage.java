package org.chuma.homecontroller.app.servlet.pages;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

import org.eclipse.jetty.server.Request;

import org.chuma.homecontroller.app.servlet.NodeTestRunner;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.Pic;
import org.chuma.homecontroller.base.node.Pin;
import org.chuma.homecontroller.controller.device.AbstractConnectedDevice;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfo;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoRegistry;

public class SystemPage extends AbstractPage {
    public static final String TARGET_SYSTEM_INFO = "/system/i";
    public static final String TARGET_SYSTEM_RESET = "/system/r";
    public static final String TARGET_SYSTEM_TEST_CYCLE = "/system/testCycle";
    public static final String TARGET_SYSTEM_TEST_ALL_ON = "/system/testAllOn";
    public static final String TARGET_SYSTEM_TEST_ALL_OFF = "/system/testAllOff";
    public static final String TARGET_SYSTEM_TEST_END = "/system/testEnd";
    private final HashMap<NodeInfo, NodeTestRunner> testRunners;
    final NodeInfoRegistry nodeInfoRegistry;

    public SystemPage(NodeInfoRegistry nodeInfoRegistry) {
        super("/system", "System", "System", "favicon.png");
        this.nodeInfoRegistry = nodeInfoRegistry;
        this.testRunners = new HashMap<>();
    }

    public String getBody(int debugNodeId) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html>").append(getHtmlHead()).append("<body><a href='").append(getRootPath())
                .append("'>Refresh</a>&nbsp;&nbsp;&nbsp;&nbsp;<a href='/'>Back</a>\n");

        builder.append("<br/><br/><table class='systemTable'><tr>");

        for (NodeInfo nodeInfo : nodeInfoRegistry.getNodeInfos()) {
            int nodeId = nodeInfo.getNode().getNodeId();

            String resetLink = "";
            String testLink = "";
            if (nodeInfo.isResetSupported()) {
                resetLink = String.format("<a href='%s%d'>reset</a>", TARGET_SYSTEM_RESET, nodeId);
                // allow test only for devices without device assigned and not on bridge
                if (nodeId != 1 && (isNodeTestRunning(nodeInfo) || nodeInfo.getNode().getDevices().isEmpty())) {
                    testLink = String.format("<a href='%s%d'>test Cycle</a> <a href='%s%d'>All ON</a> <a href='%s%d'>all OFF</a>", TARGET_SYSTEM_TEST_CYCLE, nodeId, TARGET_SYSTEM_TEST_ALL_ON, nodeId, TARGET_SYSTEM_TEST_ALL_OFF, nodeId);
                    if (isNodeTestRunning(nodeInfo)) {
                        testLink += String.format("<td><a href='%s%d'>end test</a>", TARGET_SYSTEM_TEST_END, nodeId);
                    }
                }
            }
            builder.append(String.format("<tr><td>%d-%s<td><a href='%s%d'>info</a><td>%s<td>%s", nodeId, nodeInfo.getNode().getName(), TARGET_SYSTEM_INFO, nodeId, resetLink, testLink));
        }
        builder.append("</table>");

        if (debugNodeId >= 0) {
            builder.append(printNodeDebugInfo(debugNodeId));
        }

        builder.append("</body></html>");
        return builder.toString();
    }

    private boolean isNodeTestRunning(NodeInfo nodeInfo) {
        return testRunners.get(nodeInfo) != null;
    }

    private String printNodeDebugInfo(int debugNodeId) {
        StringBuilder builder = new StringBuilder();

        Node node = nodeInfoRegistry.getNode(debugNodeId);
        builder.append(String.format("<br/><br/><div class='nodeInfoTitle'>%d-%s Detail</div>", node.getNodeId(), node.getName()));

        int[] portValues = new int[3];
        int[] trisValues = new int[3];

//        int[] readProgramMemory;
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
            builder.append(e);
            return builder.toString();
        }

//        portValues[0] = 0xFF;
//        trisValues[0] = 0xFF;
//        portValues[1] = 0x33;
//        trisValues[1] = 0x33;
//        portValues[2] = 0x11;
//        trisValues[2] = 0x11;


//        if (readProgramMemory != null && readProgramMemory.length == 4) {
//            builder.append(String.format("<br>readProgramMemory: [%x, %x, %x, %x]", readProgramMemory[0], readProgramMemory[1], readProgramMemory[2], readProgramMemory[3]));
//        }

        builder.append("<table><tr>");
        for (int connId = 1; connId <= 3; connId++) {
            builder.append("<td class='nodeInfoConnectors'>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
            appendConnectorInfo(builder, portValues, trisValues, connId);
        }
        builder.append("</table>");

        appendPicPortInfo(builder, portValues, trisValues);


        return builder.toString();
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

    private void appendConnectorInfo(StringBuilder builder, int[] portValues, int[] trisValues, int connId) {
        builder.append(String.format("<div class='nodeInfoConnectorTitle'>Conn #%d (T-V)</div>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;", connId));
        builder.append("<br/><table class='nodeInfoConnectorTable'><tr>");

        for (int row = 1; row <= 2; row++) {
            builder.append("<tr><td class='nodeInfoConnectorTable'>&nbsp;&nbsp;&nbsp;&nbsp;");
            for (int i = row; i < 7; i += 2) {
                appendConnPinDetail(builder, trisValues, portValues, connId, i);
            }
        }

        builder.append("</table>");
    }

    private void appendConnPinDetail(StringBuilder builder, int[] trisValues, int[] portValues, int connId, int pinId) {
        Pin pin = AbstractConnectedDevice.getPin(connId, pinId);
        builder.append(String.format("<td class='nodeInfoConnectorTable'>%s %d-%d",
                pin.toString().substring(3),
                applyBitMaskTo01(trisValues[pin.getPortIndex()], pin.getBitMask()),
                applyBitMaskTo01(portValues[pin.getPortIndex()], pin.getBitMask())));
    }

    private int applyBitMaskTo01(int values, int mask) {
        return ((values & mask) != 0) ? 1 : 0;
    }

    @Override
    public void handle(String target, Request request, HttpServletResponse response) throws IOException {
        int debugNodeId = tryTargetMatchAndParseArg(target, TARGET_SYSTEM_INFO);
        int resetNodeId = tryTargetMatchAndParseArg(target, TARGET_SYSTEM_RESET);
        int testCycleNodeId = tryTargetMatchAndParseArg(target, TARGET_SYSTEM_TEST_CYCLE);
        int testAllOnNodeId = tryTargetMatchAndParseArg(target, TARGET_SYSTEM_TEST_ALL_ON);
        int testAllOffNodeId = tryTargetMatchAndParseArg(target, TARGET_SYSTEM_TEST_ALL_OFF);
        int testEndNodeId = tryTargetMatchAndParseArg(target, TARGET_SYSTEM_TEST_END);

        if (resetNodeId != -1) {
            Node n = nodeInfoRegistry.getNode(resetNodeId);
            n.reset();
        } else if (testCycleNodeId >= 0) {
            startNodeTest(testCycleNodeId, NodeTestRunner.Mode.cycle);
        } else if (testAllOnNodeId >= 0) {
            startNodeTest(testAllOnNodeId, NodeTestRunner.Mode.fullOn);
        } else if (testAllOffNodeId >= 0) {
            startNodeTest(testAllOffNodeId, NodeTestRunner.Mode.fullOff);
        } else if (testEndNodeId >= 0) {
            stopNodeTest(testEndNodeId);
        }

        sendOkResponse(request, response, getBody(debugNodeId));
    }

    private void startNodeTest(int nodeId, NodeTestRunner.Mode mode) {
        synchronized (testRunners) {
            NodeInfo nodeInfo = nodeInfoRegistry.getNodeInfo(nodeId);

            NodeTestRunner testRunner = testRunners.get(nodeInfo);
            if (testRunner == null) {
                testRunner = new NodeTestRunner(nodeInfo);
                testRunners.put(nodeInfo, testRunner);
                testRunner.setMode(mode);
                testRunner.start();
            }
            testRunner.setMode(mode);
        }
    }

    private void stopNodeTest(int nodeId) {
        synchronized (testRunners) {
            NodeInfo nodeInfo = nodeInfoRegistry.getNodeInfo(nodeId);
            NodeTestRunner testRunner = testRunners.get(nodeInfo);
            if (testRunner != null) {
                testRunner.setMode(NodeTestRunner.Mode.endTest);
            }
            testRunners.remove(nodeInfo);
        }
    }
}
