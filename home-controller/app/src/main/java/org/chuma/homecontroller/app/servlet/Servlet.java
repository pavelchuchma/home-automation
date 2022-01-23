package org.chuma.homecontroller.app.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.app.configurator.AbstractConfigurator;
import org.chuma.homecontroller.app.servlet.pages.LightsPage;
import org.chuma.homecontroller.app.servlet.pages.LouversPage;
import org.chuma.homecontroller.app.servlet.pages.NodeInfoPage;
import org.chuma.homecontroller.app.servlet.pages.Page;
import org.chuma.homecontroller.app.servlet.pages.PirPage;
import org.chuma.homecontroller.app.servlet.pages.SystemPage;
import org.chuma.homecontroller.app.servlet.rest.HvacHandler;
import org.chuma.homecontroller.app.servlet.rest.LouversHandler;
import org.chuma.homecontroller.app.servlet.rest.PirHandler;
import org.chuma.homecontroller.app.servlet.rest.PwmLightsHandler;
import org.chuma.homecontroller.app.servlet.rest.ValveHandler;
import org.chuma.homecontroller.app.servlet.rest.WaterPumpHandler;
import org.chuma.homecontroller.base.node.Pin;
import org.chuma.homecontroller.controller.PirStatus;
import org.chuma.homecontroller.controller.action.Action;
import org.chuma.homecontroller.controller.actor.PwmActor;
import org.chuma.homecontroller.controller.controller.LouversController;
import org.chuma.homecontroller.controller.controller.ValveController;
import org.chuma.homecontroller.controller.device.OutputDevice;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfo;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoCollector;
import org.chuma.homecontroller.extensions.actor.HvacActor;
import org.chuma.homecontroller.extensions.actor.WaterPumpMonitor;

public class Servlet extends AbstractHandler {
    public static final List<ServletAction> rootActions = new ArrayList<>();
    private static final HashMap<String, PwmActor> pwmActorMap = new HashMap<>();
    public static List<PirStatus> pirStatusList;
    public static Map<String, LouversController> louversControllerMap;
    public static Map<String, ValveController> valveControllerMap;
    public static AbstractConfigurator configurator;
    public static HvacActor hvacActor;
    public static WaterPumpMonitor waterPumpMonitor;
    public static DecimalFormat currentValueFormatter = new DecimalFormat("###.##");
    static Logger log = LoggerFactory.getLogger(Servlet.class.getName());
    private static Action[] lightActions;
    private static LouversController[] louversControllers;
    private static ValveController[] valveControllers;
    final private List<Handler> handlers;
    final private Page defaultPage;

    public Servlet(NodeInfoCollector nodeInfoCollector) {
        HashMap<NodeInfo, NodeTestRunner> testRunners = new HashMap<>();
        handlers = Arrays.asList(
                new LightsPage(lightActions, pwmActorMap),
                new LouversPage(louversControllers, louversControllerMap),
                new SystemPage(testRunners),
                new PirPage(pirStatusList),
                new LouversHandler(Arrays.asList(louversControllers)),
                new ValveHandler(Arrays.asList(valveControllers)),
                new PwmLightsHandler(pwmActorMap.values()),
                new PirHandler(pirStatusList),
                new WaterPumpHandler(Collections.singleton(waterPumpMonitor)),
                new HvacHandler(Collections.singleton(hvacActor))
        );
        defaultPage = new NodeInfoPage(nodeInfoCollector, handlers);
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
        for (int i = 0; i < lightActions.length; i += 4) {
            PwmActor actor = (PwmActor) lightActions[i].getActor();
            PwmActor old = pwmActorMap.put(actor.getId(), actor);
            if (old != null && old != actor) {
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

    public void handle(String target, Request baseRequest, HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
        try {
            log.debug("handle: " + target);
            for (Handler handler : handlers) {
                if (handler.getRootPath().equals(target) || target.startsWith(handler.getRootPath() + "/") || target.startsWith(handler.getRootPath() + "?")) {
                    handler.handle(target, baseRequest, request, response);
                    return;
                }
            }

            if (target.endsWith(".css")) {
                sendFile(target, response, "text/css;charset=utf-8");
            } else if (target.endsWith(".html")) {
                sendFile(target, response, "text/html;charset=utf-8");
            } else if (target.endsWith(".jpg")) {
                sendFile(target, response, "image/jpeg");
            } else if (target.endsWith(".png")) {
                sendFile(target, response, "image/png");
            } else if (target.endsWith(".js")) {
                sendFile(target, response, "application/javascript;charset=utf-8");
            } else {
                defaultPage.handle(target, baseRequest, request, response);
            }
        } catch (Exception e) {
            log.error("failed to process '" + target + "'", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace(response.getWriter());
        } finally {
            baseRequest.setHandled(true);
        }
    }

    private void sendFile(String target, HttpServletResponse response, String contentType) throws IOException {
        if (!target.contains(":") && !target.contains("..")) {
            response.setContentType(contentType);

            InputStream in = this.getClass().getResourceAsStream("/servlet/content" + target);
            if (in != null) {
                if (target.endsWith(".html")) {
                    Charset charset = StandardCharsets.UTF_8;
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
}