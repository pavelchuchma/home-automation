package servlet;

import app.NodeInfo;
import app.NodeInfoCollector;
import controller.Action.Action;
import controller.device.ConnectedDevice;
import controller.device.OutputDevice;
import node.Node;
import node.Pic;
import node.Pin;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class Servlet extends AbstractHandler {
    public static final String TARGET_SYSTEM_INFO = "/system/i";
    public static final String TARGET_SYSTEM_RESET = "/system/r";
    public static final String TARGET_SYSTEM_TEST_CYCLE = "/system/testCycle";
    public static final String TARGET_SYSTEM_TEST_ALL_ON = "/system/testAllOn";
    public static final String TARGET_SYSTEM_TEST_ALL_OFF = "/system/testAllOff";
    public static final String TARGET_SYSTEM_TEST_END = "/system/testEnd";
    public static final String TARGET_SYSTEM = "/system";
    public static final String TARGET_LOUVERS_ACTION = "/zaluzie/a";
    public static final String TARGET_LOUVERS = "/zaluzie";
    private NodeInfoCollector nodeInfoCollector;
    static Logger log = Logger.getLogger(Servlet.class.getName());
    private HashMap<NodeInfo, NodeTestRunner> testRunners = new HashMap<NodeInfo, NodeTestRunner>();

    public Servlet(NodeInfoCollector nodeInfoCollector) {
        this.nodeInfoCollector = nodeInfoCollector;
    }

    private static int tryTargetMatchAndParseArg(String target, String pattern) {
        if (target.startsWith(pattern)) {
            try {
                return Integer.parseInt(target.substring(pattern.length()));
            } catch (NumberFormatException e) {
            }
        }
        return -1;
    }

    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ServletException {
        if (target.startsWith("/report.css")) {
            response.setContentType("text/css;charset=utf-8");
            baseRequest.setHandled(true);

            InputStream a = this.getClass().getResourceAsStream("/servlet/resources/report.css");
            byte[] buff = new byte[1024];
            int read;
            while ((read = a.read(buff)) > 0) {
                response.getOutputStream().write(buff, 0, read);
            }
        } else if (target.startsWith("/restart")) {
            System.exit(100);
        } else if (target.startsWith(TARGET_LOUVERS)) {
            int actionIndex = tryTargetMatchAndParseArg(target, TARGET_LOUVERS_ACTION);
            if (actionIndex != -1) {
                zaluezieActions[actionIndex].perform();
            }

            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            response.getWriter().println(getZaluziePage());

        } else if (target.startsWith(TARGET_SYSTEM)) {
            int debugNodeId = tryTargetMatchAndParseArg(target, TARGET_SYSTEM_INFO);
            int resetNodeId = tryTargetMatchAndParseArg(target, TARGET_SYSTEM_RESET);
            int testCycleNodeId = tryTargetMatchAndParseArg(target, TARGET_SYSTEM_TEST_CYCLE);
            int testAllOnNodeId = tryTargetMatchAndParseArg(target, TARGET_SYSTEM_TEST_ALL_ON);
            int testAllOffNodeId = tryTargetMatchAndParseArg(target, TARGET_SYSTEM_TEST_ALL_OFF);
            int testEndNodeId = tryTargetMatchAndParseArg(target, TARGET_SYSTEM_TEST_END);

            if (resetNodeId != -1) {
                Node n = NodeInfoCollector.getInstance().getNode(resetNodeId);
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

            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            response.getWriter().println(getSystemPage(debugNodeId));

        } else {
            if (target.startsWith("/a")) {
                processAction(target);
            }
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            response.getWriter().println(nodeInfoCollector.getReport());
        }
    }

    public static void startServer(NodeInfoCollector nodeInfoCollector) throws Exception {
        Server server = new Server(80);
        server.setHandler(new Servlet(nodeInfoCollector));

        server.start();
        server.join();
    }

    public static Action action1;
    public static Action action2;
    public static Action action3;
    public static Action action4;
    public static Action action5;
    public static Action[] zaluezieActions;

    private void processAction(String action) {
        if (action.startsWith("/a1") && action1 != null) {
            action1.perform();
        }
        if (action.startsWith("/a2") && action2 != null) {
            action2.perform();
        }
        if (action.startsWith("/a3") && action3 != null) {
            action3.perform();
        }
        if (action.startsWith("/a4") && action4 != null) {
            action4.perform();
        }
        if (action.startsWith("/a5") && action5 != null) {
            action5.perform();
        }
    }

    private String getZaluziePage() {
        StringBuilder builder = new StringBuilder();

        builder.append("<html>" +
                "<head>" +
                "<link href='/report.css' rel='stylesheet' type='text/css'/>\n" +
                "</head>" +
                "<body><a href='" + TARGET_LOUVERS + "'>Refresh</a>&nbsp;&nbsp;&nbsp;&nbsp;<a href='/'>Back</a>\n");

        builder.append(getZaluzieTable(0, 6));
        builder.append(getZaluzieTable(6, 6));
        builder.append(getZaluzieTable(12, 6));

        builder.append("</body></html>");
        return builder.toString();
    }

    private String getSystemPage(int debugNodeId) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html>" +
                "<head>" +
                "<link href='/report.css' rel='stylesheet' type='text/css'/>\n" +
                "</head>" +
                "<body><a href='" + TARGET_SYSTEM + "'>Refresh</a>&nbsp;&nbsp;&nbsp;&nbsp;<a href='/'>Back</a>\n");

        builder.append("<br/><br/><table class='systemTable'><tr>");

        for (NodeInfo nodeInfo : nodeInfoCollector) {
            int nodeId = nodeInfo.getNode().getNodeId();

            String resetLink = "";
            String testLink = "";
            if (nodeInfo.isResetSupported()) {
                resetLink = String.format("<a href='%s%d'>reset</a>", TARGET_SYSTEM_RESET, nodeId);
                // allow test only for devices without device assigned
                if (isNodeTestRunning(nodeInfo) || nodeInfo.getNode().getDevices().isEmpty()) {
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

    private int applyBitMaskTo01(int values, int mask) {
        return ((values & mask) != 0) ? 1 : 0;
    }

    private String printNodeDebugInfo(int debugNodeId) {
        StringBuilder builder = new StringBuilder();

        Node node = nodeInfoCollector.getNode(debugNodeId);
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

    private void appendConnPinDetail(StringBuilder builder, int trisValues[], int portValues[], int connId, int pinId) {
        Pin pin = ConnectedDevice.getPin(connId, pinId);
        builder.append(String.format("<td class='nodeInfoConnectorTable'>%s %d-%d",
                pin.toString().substring(3),
                applyBitMaskTo01(trisValues[pin.getPortIndex()], pin.getBitMask()),
                applyBitMaskTo01(portValues[pin.getPortIndex()], pin.getBitMask())));
    }


    private String getZaluzieTable(int startIndex, int count) {
        StringBuilder builder = new StringBuilder();
        builder.append("<br/><br/><table class='buttonTable'>");
        for (int row = 0; row < 2; row++) {
            builder.append("<tr>");
            for (int i = startIndex + row; i < startIndex + count; i += 2) {
                String fieldClass = (zaluezieActions[i].getActor().getValue() == 0) ? "louvers" : "louversRunning";
                builder.append(String.format("<td class='%s'><a href='%s%d'>%s</a>", fieldClass, TARGET_LOUVERS_ACTION, i, zaluezieActions[i].getActor().getId()));
            }
        }
        builder.append("</table>");
        return builder.toString();
    }

    Pin[] getOutputDevicePins(OutputDevice dev) {
        return new Pin[]{
                dev.getOut5().getPin(),
                dev.getOut3().getPin(),
                dev.getOut1().getPin(),
                dev.getOut2().getPin(),
                dev.getOut4().getPin(),
                dev.getOut6().getPin()
        };
    }

    private void startNodeTest(int nodeId, NodeTestRunner.Mode mode) throws IOException {
        synchronized (testRunners) {
            NodeInfo nodeInfo = NodeInfoCollector.getInstance().getNodeInfo(nodeId);

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
            NodeInfo nodeInfo = NodeInfoCollector.getInstance().getNodeInfo(nodeId);
            NodeTestRunner testRunner = testRunners.get(nodeInfo);
            if (testRunner != null) {
                testRunner.setMode(NodeTestRunner.Mode.endTest);
            }
            testRunners.remove(nodeInfo);
        }
    }

    private boolean isNodeTestRunning(NodeInfo nodeInfo) {
        return testRunners.get(nodeInfo) != null;
    }
}