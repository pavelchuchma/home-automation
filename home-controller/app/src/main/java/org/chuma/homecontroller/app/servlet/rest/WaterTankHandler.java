package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.extensions.external.watertank.WaterTankMonitor;

/**
 * Water tank level pushed by the water-level-meter ESP.
 * <ul>
 * <li>{@code POST /rest/wtank/push} with form parameters {@code distance} (mm), {@code status}
 * ({@code OK}/{@code FAIL}) and optional {@code samples}, {@code validSamples}, {@code rssi},
 * {@code resetReason} records a reading. Responds 200, or 400 on a missing/invalid parameter.</li>
 * <li>{@code GET /rest/wtank/status} reports the fill percentage and the last pushed reading.</li>
 * </ul>
 */
public class WaterTankHandler extends AbstractRestHandler<WaterTankMonitor> {
    private static final Logger log = LoggerFactory.getLogger(WaterTankHandler.class);

    private final WaterTankMonitor monitor;
    private final String pushPath;

    public WaterTankHandler(WaterTankMonitor monitor) {
        super("wtank", monitor);
        this.monitor = monitor;
        this.pushPath = rootPath + "/push";
    }

    @Override
    public void handle(String target, Request request, HttpServletResponse response) throws IOException {
        if (target.startsWith(pushPath)) {
            handlePush(request, response);
            request.setHandled(true);
        } else {
            super.handle(target, request, response);
        }
    }

    private void handlePush(Request request, HttpServletResponse response) throws IOException {
        if (!"POST".equals(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            response.getWriter().print("POST expected");
            return;
        }
        Map<String, String[]> params = request.getParameterMap();
        WaterTankMonitor.Reading reading;
        try {
            reading = monitor.newReading(
                    getMandatoryIntParam(params, "distance"),
                    getMandatoryStringParam(params, "status"),
                    getIntParam(params, "samples", 0),
                    getIntParam(params, "validSamples", 0),
                    getIntParam(params, "rssi", 0),
                    getStringParam(params, "resetReason"));
        } catch (IllegalArgumentException e) {
            log.warn("Rejected water tank push from {}: {}", request.getRemoteAddr(), e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print(e.getMessage());
            return;
        }
        monitor.recordReading(reading);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print("OK");
    }

    @Override
    void writeJsonItemValues(JsonWriter jw, WaterTankMonitor monitor, HttpServletRequest request) {
        jw.addAttribute("fillPercent", monitor.getFillPercent());
        WaterTankMonitor.Reading r = monitor.getLastReading();
        if (r == null) {
            return;
        }
        jw.addAttribute("distanceMm", r.distanceMm);
        jw.addAttribute("status", r.status);
        jw.addAttribute("ageSec", monitor.getLastReadingAgeMs() / 1000);
        jw.addAttribute("samples", r.samples);
        jw.addAttribute("validSamples", r.validSamples);
        jw.addAttribute("rssi", r.rssi);
        if (r.resetReason != null) {
            jw.addAttribute("resetReason", r.resetReason);
        }
    }
}
