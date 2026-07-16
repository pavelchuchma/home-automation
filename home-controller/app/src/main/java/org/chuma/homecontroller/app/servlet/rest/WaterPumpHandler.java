package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;

import java.util.Map;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.extensions.actor.WaterPumpMonitor;
import org.chuma.homecontroller.extensions.external.watertank.WaterTankMonitor;

public class WaterPumpHandler extends AbstractRestHandler<WaterPumpMonitor> {
    private final WaterTankMonitor tankMonitor;

    public WaterPumpHandler(WaterPumpMonitor monitor, WaterTankMonitor tankMonitor) {
        super("wpump", monitor);
        this.tankMonitor = tankMonitor;
    }

    @Override
    void writeJsonItemValues(JsonWriter jw, WaterPumpMonitor monitor, HttpServletRequest request) {
        final Map<String, String[]> parameterMap = request.getParameterMap();
        int lastHours = getIntParam(parameterMap, "lastHours", 24);
        int recordCount = getIntParam(parameterMap, "recordCount", 5);

        jw.addAttribute("on", monitor.isOn());
        jw.addAttribute("recCount", monitor.getRecordCount());
        jw.addAttribute("lastPeriodRecCount", monitor.getRecordCountInLastHours(lastHours));
        jw.addAttribute("tankFillPercent", tankMonitor.getFillPercent());
        if (recordCount > 0) {
            try (JsonWriter arr = jw.startArrayAttribute("lastRecords")) {
                for (WaterPumpMonitor.Record r : monitor.getLastRecords(recordCount)) {
                    try (JsonWriter rw = arr.startObject()) {
                        rw.addAttribute("time", r.time.toString());
                        rw.addAttribute("duration", r.duration);
                    }
                }
            }
        }
    }
}
