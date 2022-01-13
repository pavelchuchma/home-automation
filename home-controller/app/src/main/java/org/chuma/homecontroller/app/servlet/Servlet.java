package org.chuma.homecontroller.app.servlet;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.chuma.homecontroller.app.configurator.AbstractConfigurator;
import org.chuma.homecontroller.controller.PirStatus;
import org.chuma.homecontroller.controller.action.Action;
import org.chuma.homecontroller.controller.actor.PwmActor;
import org.chuma.homecontroller.controller.controller.Activity;
import org.chuma.homecontroller.controller.controller.LouversController;
import org.chuma.homecontroller.controller.controller.ValveController;
import org.chuma.homecontroller.controller.device.AbstractConnectedDevice;
import org.chuma.homecontroller.controller.device.OutputDevice;
import org.chuma.homecontroller.controller.nodeinfo.LogMessage;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfo;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoCollector;
import org.chuma.homecontroller.extensions.actor.HvacActor;
import org.chuma.homecontroller.extensions.actor.WaterPumpMonitor;
import org.chuma.homecontroller.base.node.MessageType;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.Pic;
import org.chuma.homecontroller.base.node.Pin;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Servlet extends AbstractHandler {
    public static final String TARGET_SYSTEM_INFO = "/system/i";
    public static final String TARGET_SYSTEM_RESET = "/system/r";
    public static final String TARGET_SYSTEM_TEST_CYCLE = "/system/testCycle";
    public static final String TARGET_SYSTEM_TEST_ALL_ON = "/system/testAllOn";
    public static final String TARGET_SYSTEM_TEST_ALL_OFF = "/system/testAllOff";
    public static final String TARGET_SYSTEM_TEST_END = "/system/testEnd";
    public static final String TARGET_SYSTEM = "/system";
    public static final String TARGET_PIR_STATUS_PAGE = "/pirStatus";
    public static final String TARGET_LIGHTS = "/lights";
    public static final String TARGET_LIGHTS_STATUS = TARGET_LIGHTS + "/status";
    public static final String TARGET_LIGHTS_ACTION = TARGET_LIGHTS + "/a";
    public static final String TARGET_LIGHTS_PARAM_ACTION = TARGET_LIGHTS + "/action";
    public static final String TARGET_LIGHTS_OBYVAK = "/lightsObyvak.html";
    public static final String TARGET_HVAC = "/hvac";
    public static final String TARGET_HVAC_STATUS = TARGET_HVAC + "/status";
    public static final String TARGET_HVAC_ACTION = TARGET_HVAC + "/action";
    public static final String TARGET_WPUMP = "/wpump";
    public static final String TARGET_WPUMP_STATUS = TARGET_WPUMP + "/status";
    public static final String TARGET_LOUVERS = "/louvers";
    public static final String TARGET_LOUVERS_STATUS = TARGET_LOUVERS + "/status";
    public static final String TARGET_LOUVERS_ACTION = TARGET_LOUVERS + "/a";
    public static final String TARGET_LOUVERS_PARAM_ACTION = TARGET_LOUVERS + "/action";
    public static final String TARGET_VALVES = "/airvalves";
    public static final String TARGET_VALVES_STATUS = TARGET_VALVES + "/status";
    public static final String TARGET_VALVES_PARAM_ACTION = TARGET_VALVES + "/action";
    public static final String CLASS_LOUVERS_ARROW = "louversArrow";
    public static final String CLASS_LOUVERS_ARROW_ACTIVE = "louversArrow louversArrow-moving";
    public static final String TARGET_PIR = "/pir";
    public static final String TARGET_PIR_STATUS = TARGET_PIR + "/status";
    public static Action action1;
    public static Action action2;
    public static Action action3;
    public static Action action4;
    public static Action action5;
    public static List<PirStatus> pirStatusList;
    public static Map<String, LouversController> louversControllerMap;
    public static Map<String, ValveController> valveControllerMap;
    public static AbstractConfigurator configurator;
    public static HvacActor hvacActor;
    public static Action hvacOnAction;
    public static Action hvacOffAction;
    public static WaterPumpMonitor waterPumpMonitor;
    static Logger log = LoggerFactory.getLogger(Servlet.class.getName());
    private static Action[] lightActions;
    private static Map<String, PwmActor> lightActorMap;
    private static LouversController[] louversControllers;
    private static ValveController[] valveControllers;
    private static DecimalFormat currentValueFormatter = new DecimalFormat("###.##");
    private NodeInfoCollector nodeInfoCollector;
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

    public static void startServer(NodeInfoCollector nodeInfoCollector) throws Exception {
        log.info("Starting web server");
        Server server = new Server(80);
        server.setHandler(new Servlet(nodeInfoCollector));

        server.start();
        log.info("Web server started");
        server.join();
    }

    public static void setLightActions(Action[] lightActions) {
        Servlet.lightActions = lightActions;
        lightActorMap = new HashMap<>();
        for (int i = 0; i < lightActions.length; i += 4) {
            PwmActor actor = (PwmActor) lightActions[i].getActor();
            if (lightActorMap.put(actor.getId(), actor) != null) {
                throw new RuntimeException("Id of actor '" + actor.getId() + "' is not unique");
            }
        }
    }

    public static void setLouversControllers(LouversController[] louversControllers) {
        Servlet.louversControllers = louversControllers;
        louversControllerMap = new HashMap<>();
        for (LouversController controller : louversControllers) {
            if (louversControllerMap.put(controller.getId(), controller) != null) {
                throw new RuntimeException("Id of controller '" + controller.getId() + "' is not unique");
            }
        }
    }

    public static void setValveControllers(ValveController[] valveControllers) {
        Servlet.valveControllers = valveControllers;
        valveControllerMap = new HashMap<>();
        for (ValveController controller : valveControllers) {
            if (valveControllerMap.put(controller.getId(), controller) != null) {
                throw new RuntimeException("Id of controller '" + controller.getId() + "' is not unique");
            }
        }
    }

    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ServletException {

        try {
            log.debug("handle: " + target);
            if (target.endsWith(".css")) {
                sendFile(target, response, "text/css;charset=utf-8");
                baseRequest.setHandled(true);
            } else if (target.endsWith(".html")) {
                sendFile(target, response, "text/html;charset=utf-8");
                baseRequest.setHandled(true);
            } else if (target.endsWith(".jpg")) {
                sendFile(target, response, "image/jpeg");
                baseRequest.setHandled(true);
            } else if (target.endsWith(".png")) {
                sendFile(target, response, "image/png");
                baseRequest.setHandled(true);
            } else if (target.endsWith(".js")) {
                sendFile(target, response, "application/javascript;charset=utf-8");
                baseRequest.setHandled(true);
            } else if (target.startsWith(TARGET_PIR_STATUS)) {
                writePitStatusJson(baseRequest, response);
            } else if (target.startsWith(TARGET_VALVES)) {
                if (target.startsWith(TARGET_VALVES_STATUS)) {
                    writeAirValvesStatusJson(baseRequest, response);
                } else if (target.startsWith(TARGET_VALVES_PARAM_ACTION)) {
                    ValveController controller = getItemById(request, valveControllerMap);

                    String posStr = request.getParameter("val");
                    if (posStr != null) {
                        int position = Integer.parseInt(posStr);
                        controller.setPosition(position);
                        sendOkResponse(baseRequest, response, "");
                    }
                }
            } else {
                if (target.startsWith(TARGET_LOUVERS)) {
                    if (target.startsWith(TARGET_LOUVERS_STATUS)) {
                        writeLouversStatusJson(baseRequest, response);
                    } else if (target.startsWith(TARGET_LOUVERS_PARAM_ACTION)) {
                        LouversController controller = getItemById(request, louversControllerMap);

                        String posStr = request.getParameter("pos");
                        String offStr = request.getParameter("off");
                        if (posStr != null && offStr != null) {
                            int position = Integer.parseInt(posStr);
                            int offset = Integer.parseInt(offStr);
                            if (position == 0) {
                                controller.up();
                            } else if (position == 100) {
                                controller.outshine(offset);
                            }
                            sendOkResponse(baseRequest, response, "");
                        }
                    } else {
                        int actionIndex = tryTargetMatchAndParseArg(target, TARGET_LOUVERS_ACTION);
                        if (actionIndex != -1) {
                            processLouversAction(actionIndex);
                        }

                        sendOkResponse(baseRequest, response, getLouversPage());
                    }

                } else if (target.startsWith(TARGET_LIGHTS)) {
                    if (target.startsWith(TARGET_LIGHTS_STATUS)) {
                        writeLightsStatusJson(baseRequest, response);
                    } else if (target.startsWith(TARGET_LIGHTS_PARAM_ACTION)) {
                        PwmActor actor = getItemById(request, lightActorMap);

                        String valStr = request.getParameter("val");
                        if (valStr != null) {
                            int value = Integer.parseInt(valStr);
                            actor.setValue(value, null);
                            sendOkResponse(baseRequest, response, "");
                        }
                    } else {
                        int actionIndex = tryTargetMatchAndParseArg(target, TARGET_LIGHTS_ACTION);
                        if (actionIndex != -1) {
                            lightActions[actionIndex].perform(-1);
                        }

                        sendOkResponse(baseRequest, response, getLightsPage());
                    }
                } else if (target.startsWith(TARGET_PIR_STATUS_PAGE)) {
                    sendOkResponse(baseRequest, response, getPirPage());

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

                    sendOkResponse(baseRequest, response, getSystemPage(debugNodeId));
                } else if (target.startsWith(TARGET_HVAC_STATUS)) {
                    writeHvacStatusJson(baseRequest, response);
                } else if (target.startsWith(TARGET_HVAC_ACTION)) {
                    if ("hvac".equals(request.getParameter("id"))) {
                        if ("true".equals(request.getParameter("on"))) {
                            hvacOnAction.perform(0);
                        } else {
                            hvacOffAction.perform(0);
                        }
                    }

                    sendOkResponse(baseRequest, response, getLouversPage());

                } else if (target.startsWith(TARGET_WPUMP_STATUS)) {
                    writeWaterPumpStatusJson(baseRequest, response);
                } else {
                    if (target.startsWith("/a")) {
                        processAction(target);
                    }
                    sendOkResponse(baseRequest, response, getNodeInfoReport(nodeInfoCollector));
                }
            }
        } catch (Exception e) {
            log.error("failed to process '" + target + "'", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace(response.getWriter());
            baseRequest.setHandled(true);
        }
    }

    private static String getNodeInfoReport(NodeInfoCollector collector) {
        StringBuilder builder = new StringBuilder();

        builder.append("<html>" +
                "<meta http-equiv='refresh' content='1;url=/'/>" +
                getHtmlHead() +
                "<body>");

        String[] actionNames = new String[]{"Bzucak", "Garaz", "Jidelna", "Zvuk"};
        Action[] actions = new Action[]{Servlet.action1, Servlet.action1, Servlet.action2, Servlet.action3, Servlet.action4};
        for (int i = 0; i < actionNames.length; i++) {
            if (actions[i] != null) {
                builder.append(String.format("<a href='/a%d'>%s</a>&nbsp;&nbsp;&nbsp;&nbsp;", i + 1, actionNames[i]));
            }
        }

        builder.append("<a href='" + Servlet.TARGET_LOUVERS + "'>Zaluzie...</a>&nbsp;&nbsp;&nbsp;&nbsp;");
        builder.append("<a href='" + Servlet.TARGET_LIGHTS + "'>Svetla...</a>&nbsp;&nbsp;&nbsp;&nbsp;");
        builder.append("<a href='" + Servlet.TARGET_SYSTEM + "'>System...</a>&nbsp;&nbsp;&nbsp;&nbsp;");
        builder.append("<a href='" + Servlet.TARGET_PIR_STATUS_PAGE + "'>Pir Status...</a>&nbsp;&nbsp;&nbsp;&nbsp;");
        builder.append("<a href='" + Servlet.TARGET_LIGHTS_OBYVAK + "'>Obyvak...</a>&nbsp;&nbsp;&nbsp;&nbsp;");

        builder.append("<table class='nodeTable'>\n" +
                "<tr><th class=''>Node #<th class=''>Last Ping Time<th class=''>Boot Time<th class=''>Build Time<th class=''>MessageLog");

        for (NodeInfo info : collector.getNodeInfoArray()) {
            if (info != null) {
                String lastPingClass = "errorValue";
                String lastPingString = "-";
                long lastPing;
                if (info.getLastPingTime() != null) {
                    lastPing = (new Date().getTime() - info.getLastPingTime().getTime()) / 1000;
                    if (lastPing <= Node.HEART_BEAT_PERIOD) lastPingClass = "fineValue";
                    lastPingString = lastPing + " s";
                }
                builder.append(String.format("<tr><td>%d-%s<td class='%s'>%s<td>%s<td>%s<td class='messageLog'>",
                        info.getNode().getNodeId(), info.getNode().getName(), lastPingClass, lastPingString,
                        info.getBuildTime(), info.getBuildTime()));

                for (LogMessage m : info.getMessageLog()) {
                    builder.append(String.format("<div class='%s'>%s%s</div>",
                            (m.received) ? "receivedMessage" : "sentMessage",
                            MessageType.toString(m.packet.messageType),
                            (m.packet.data != null) ? Arrays.toString(m.packet.data) : ""));
                }
                builder.append("\n");
            }
        }
        builder.append("</table>");
        builder.append("</body></html>");
        return builder.toString();
    }

    private static String getHtmlHead() {
        return "<head>" +
                "<link rel='icon' type='image/png' href='favicon.png'>" +
                "<link href='report.css' rel='stylesheet' type='text/css'/>" +
                "</head>";
    }


    private void writeHvacStatusJson(Request baseRequest, HttpServletResponse response) throws IOException {
        initJsonResponse(baseRequest, response);
        StringBuffer b = new StringBuffer();
        b.append("{ \"hvac\" : [\n");
        b.append(HvacJsonSerializer.serialize(hvacActor.getHvacDevice()));
        b.append("\n]}");
        response.getWriter().println(b);
    }

    private void writeWaterPumpStatusJson(Request baseRequest, HttpServletResponse response) throws IOException {
        initJsonResponse(baseRequest, response);
        StringBuffer b = new StringBuffer();
        b.append("{ \"wpmp\" : [\n");
        b.append(WaterPumpJsonSerializer.serialize(waterPumpMonitor, 20, 24));
        b.append("\n]}");
        response.getWriter().println(b);
    }

    private void writePitStatusJson(Request baseRequest, HttpServletResponse response) throws IOException {
        log.debug("writePitStatusJson");
        initJsonResponse(baseRequest, response);
        StringBuffer b = new StringBuffer();
        b.append("{ \"pir\" : [\n");
        boolean first = true;
        for (PirStatus ps : pirStatusList) {
            if (first) {
                first = false;
            } else {
                b.append(",\n");
            }
            b.append('{');
            appendNameValue(b, "id", ps.getId());
            b.append(',');
            appendNameValue(b, "name", ps.getName());
            b.append(',');
            appendNameValue(b, "active", (ps.isActive()) ? "1" : "0");
            b.append(',');
            long age = (ps.getLastActivate() != null) ? (new Date().getTime() - ps.getLastActivate().getTime()) / 1000 : -1;
            appendNameValue(b, "age", String.valueOf(age));
            b.append('}');
        }
        b.append("\n]}");
        response.getWriter().println(b);
    }

    private <T> T getItemById(HttpServletRequest request, Map<String, T> map) {
        String id = request.getParameter("id");
        Validate.notNull(id, "no id specified");
        T item = map.get(id);
        Validate.notNull(item, "unknown id");
        return item;
    }

    private void sendOkResponse(Request baseRequest, HttpServletResponse response, String body) throws IOException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        response.getWriter().println(body);
    }

    private void sendFile(String target, HttpServletResponse response, String contentType) throws IOException {
        if (!target.contains(":") && !target.contains("..")) {
            response.setContentType(contentType);

            InputStream in = this.getClass().getResourceAsStream("/servlet/content" + target);
            if (in != null) {
                if (target.endsWith(".html")) {
                    Charset charset = Charset.forName("utf-8");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), charset));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        writer.write(line.replace("configuration-pi.js", configurator.getConfigurationJs()));
                        writer.write('\n');
                    }
                    writer.close();
                } else {
                    byte[] buff = new byte[1024];
                    int read;
                    while ((read = in.read(buff)) > 0) {
                        response.getOutputStream().write(buff, 0, read);
                    }
                }
                return;
            }
        }
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    private void writeLightsStatusJson(Request baseRequest, HttpServletResponse response) throws IOException {
        initJsonResponse(baseRequest, response);
        StringBuffer b = new StringBuffer();
        b.append("{ \"lights\" : [\n");
        boolean first = true;
        for (PwmActor actor : lightActorMap.values()) {
            if (first) {
                first = false;
            } else {
                b.append(",\n");
            }
            b.append('{');
            appendNameValue(b, "id", actor.getId());
            b.append(',');
            appendNameValue(b, "name", actor.getLabel());
            b.append(',');
            appendNameValue(b, "val", Integer.toString(actor.getPwmValue()));
            b.append(',');
            appendNameValue(b, "maxVal", Integer.toString(actor.getMaxPwmValue()));
            b.append(',');
            appendNameValue(b, "curr", currentValueFormatter.format(actor.getOutputCurrent()));
            b.append('}');
        }
        b.append("\n]}");
        response.getWriter().println(b);
    }

    private void writeLouversStatusJson(Request baseRequest, HttpServletResponse response) throws IOException {
        log.debug("writeLouversStatusJson");
        initJsonResponse(baseRequest, response);
        StringBuffer b = new StringBuffer();
        b.append("{ \"louvers\" : [\n");
        boolean first = true;
        for (LouversController lc : louversControllers) {
            if (first) {
                first = false;
            } else {
                b.append(",\n");
            }
            b.append('{');
            appendNameValue(b, "id", lc.getId());
            b.append(',');
            appendNameValue(b, "name", lc.getLabel());
            b.append(',');
            appendNameValue(b, "pos", Double.toString(lc.getPosition()));
            b.append(',');
            appendNameValue(b, "off", Double.toString(lc.getOffset()));
            b.append(',');
            appendNameValue(b, "act", lc.getActivity().toString());
            b.append('}');
        }
        b.append("\n]}");
        response.getWriter().println(b);
    }

    private void writeAirValvesStatusJson(Request baseRequest, HttpServletResponse response) throws IOException {
        log.debug("writeAirValvesStatusJson");
        initJsonResponse(baseRequest, response);
        StringBuffer b = new StringBuffer();
        b.append("{ \"airValves\" : [\n");
        boolean first = true;
        for (ValveController lc : valveControllers) {
            if (first) {
                first = false;
            } else {
                b.append(",\n");
            }
            b.append('{');
            appendNameValue(b, "id", lc.getId());
            b.append(',');
            appendNameValue(b, "name", lc.getLabel());
            b.append(',');
            appendNameValue(b, "pos", Double.toString(lc.getPosition()));
            b.append(',');
            appendNameValue(b, "act", lc.getActivity().toString());
            b.append('}');
        }
        b.append("\n]}");
        response.getWriter().println(b);
    }

    private void initJsonResponse(Request baseRequest, HttpServletResponse response) {
        response.setContentType("application/json;charset=utf-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
    }

    private void appendNameValue(StringBuffer b, String name, String value) {
        b.append("\"").append(name).append("\":\"").append(value).append("\"");
    }

    private void processLouversAction(int actionIndex) {
        LouversController lc = louversControllers[actionIndex / 3];
        new Thread(() -> {
            processLouversActionImpl(lc, actionIndex % 3);
        }).start();
    }

    private void processLouversActionImpl(LouversController lc, int action) {
        switch (action) {
            case 0:
                if (lc.getActivity() == Activity.movingUp) {
                    lc.stop();
                } else {
                    lc.up();
                }
                break;
            case 1:
                lc.outshine(0);
                break;
            case 2:
                if (lc.getActivity() == Activity.movingDown) {
                    lc.stop();
                } else {
                    lc.blind();
                }
        }
    }

    private void processAction(String action) {
        if (action.startsWith("/a1") && action1 != null) {
            action1.perform(-1);
        }
        if (action.startsWith("/a2") && action2 != null) {
            action2.perform(-1);
        }
        if (action.startsWith("/a3") && action3 != null) {
            action3.perform(-1);
        }
        if (action.startsWith("/a4") && action4 != null) {
            action4.perform(-1);
        }
        if (action.startsWith("/a5") && action5 != null) {
            action5.perform(-1);
        }
    }

    private String getLouversPage() {
        StringBuilder builder = new StringBuilder();

        builder.append("<html>" +
                getHtmlHead() +
                "<body><a href='" + TARGET_LOUVERS + "'>Refresh</a>&nbsp;&nbsp;&nbsp;&nbsp;<a href='/'>Back</a>\n");

        for (int i = 0; i < louversControllers.length; i += 4) {
            int count = (i + 8 < louversControllers.length) ? 4 : louversControllers.length - i;
            builder.append(getLouversTable(i, count));
            if (count > 4) {
                break;
            }
        }
//        builder.append(getLouversTable(0, 4));
//        builder.append(getLouversTable(4, 4));
//        builder.append(getLouversTable(8, 4));
//        builder.append(getLouversTable(12, 4));
//        builder.append(getLouversTable(16, 5));

        builder.append("</body></html>");
        return builder.toString();
    }

    private String getLightsPage() {
        StringBuilder builder = new StringBuilder();

        builder.append("<html>" +
                getHtmlHead() +
                "<body><a href='" + TARGET_LIGHTS + "'>Refresh</a>&nbsp;&nbsp;&nbsp;&nbsp;<a href='/'>Back</a>\n");

        builder.append("<br/><br/><table class='buttonTable'>");
        for (int i = 0; i < lightActions.length; i += 4) {
            builder.append("<tr>");
            PwmActor actor = (PwmActor) lightActions[i].getActor();
            String fieldClass = "louvers";
            String stateFieldClass = (actor.isOn()) ? "louversRunning" : "louvers";
            builder.append(String.format("<td class='%s'><a href='%s%d'>%s</a>", fieldClass, TARGET_LIGHTS_ACTION, i, "On"));
            builder.append(String.format("<td class='%s'><a href='%s%d'>%s</a>", fieldClass, TARGET_LIGHTS_ACTION, i + 1, "+"));
            builder.append(String.format("<td title='%s, max %s A' class='%s'>%s %d%% <div class='gray'>(%d/%d) %sA</div>",
                    actor.getLddOutput().getDeviceName(), currentValueFormatter.format(actor.getMaxOutputCurrent()),
                    stateFieldClass, lightActions[i].getActor().getLabel(), actor.getValue(), actor.getPwmValue(), actor.getMaxPwmValue(),
                    currentValueFormatter.format(actor.getOutputCurrent())));
            builder.append(String.format("<td class='%s'><a href='%s%d'>%s</a>", fieldClass, TARGET_LIGHTS_ACTION, i + 2, "-"));
            builder.append(String.format("<td class='%s'><a href='%s%d'>%s</a>", fieldClass, TARGET_LIGHTS_ACTION, i + 3, "Off"));
        }
        builder.append("</table>");


        builder.append("</body></html>");
        return builder.toString();
    }

    private String getPirPage() {
        StringBuilder builder = new StringBuilder();

        builder.append("<html>" +
                "<meta http-equiv='refresh' content='1;url=" + TARGET_PIR_STATUS_PAGE + "'/>" +
                getHtmlHead() +
                "<body><a href='" + TARGET_PIR_STATUS_PAGE + "'>Refresh</a>&nbsp;&nbsp;&nbsp;&nbsp;<a href='/'>Back</a>\n");

        builder.append("<br/><br/><table class='buttonTable'>");
        for (PirStatus status : pirStatusList) {
            builder.append("<tr>");
            String stateFieldClass = (status.isActive()) ? "louversRunning" : "louvers";
            builder.append(String.format("<td class='%s'>%s", stateFieldClass, status.getName()));
            String fieldClass = "louvers";
            builder.append(String.format("<td class='%s'>%s", fieldClass, status.getLastActivate()));
        }
        builder.append("</table>");

        builder.append("</body></html>");
        return builder.toString();
    }

    private String getSystemPage(int debugNodeId) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html>" +
                getHtmlHead() +
                "<body><a href='" + TARGET_SYSTEM + "'>Refresh</a>&nbsp;&nbsp;&nbsp;&nbsp;<a href='/'>Back</a>\n");

        builder.append("<br/><br/><table class='systemTable'><tr>");

        for (NodeInfo nodeInfo : nodeInfoCollector) {
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
        Pin pin = AbstractConnectedDevice.getPin(connId, pinId);
        builder.append(String.format("<td class='nodeInfoConnectorTable'>%s %d-%d",
                pin.toString().substring(3),
                applyBitMaskTo01(trisValues[pin.getPortIndex()], pin.getBitMask()),
                applyBitMaskTo01(portValues[pin.getPortIndex()], pin.getBitMask())));
    }


    private String getLouversTable(int startIndex, int count) {
        StringBuilder builder = new StringBuilder();
        builder.append("<br/><br/><table class='buttonTable'>");
        for (int i = startIndex; i < startIndex + count; i++) {
            LouversController lc = louversControllers[i];
            String upArrow = (lc.isUp()) ? "▲" : "△";
            String downArrow = (lc.isDown()) ? "▼" : "▽";
            String outshineCharacter = "☀";

            Activity activity = lc.getActivity();
            String upArrowClazz = (activity == Activity.movingUp) ? CLASS_LOUVERS_ARROW_ACTIVE : CLASS_LOUVERS_ARROW;
            String downArrowClazz = (activity == Activity.movingDown) ? CLASS_LOUVERS_ARROW_ACTIVE : CLASS_LOUVERS_ARROW;
            String outShineClazz = CLASS_LOUVERS_ARROW;
            builder.append("    <td class='louversItem'>\n" +
                    "        <table>\n");

            appendLouversIcon(builder, upArrow, 3 * i, upArrowClazz);
            appendLouversIcon(builder, outshineCharacter, 3 * i + 1, outShineClazz);
            appendLouversIcon(builder, downArrow, 3 * i + 2, downArrowClazz);

            builder.append(String.format("<tr>" +
                    "            <td colspan=\"3\" class='louversName'>%s<tr>\n" +
                    "        </table>\n", lc.getLabel()));

        }
        builder.append("</table>");
        return builder.toString();
    }

    private void appendLouversIcon(StringBuilder builder, String icon, int linkAction, String clazz) {
        builder.append(String.format("<td onClick=\"document.location.href='%s%s'\" class='%s'>%s\n", TARGET_LOUVERS_ACTION, linkAction, clazz, icon));
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