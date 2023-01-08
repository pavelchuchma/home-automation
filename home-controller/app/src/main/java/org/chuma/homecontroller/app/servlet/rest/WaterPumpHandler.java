package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;

import java.util.Map;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.extensions.actor.WaterPumpMonitor;

public class WaterPumpHandler extends AbstractRestHandler<WaterPumpMonitor> {
    public WaterPumpHandler(Iterable<WaterPumpMonitor> monitors) {
        super("wpump", "wpump", monitors, (o) -> "wpump");
    }

    @Override
    void writeJsonItemValues(JsonWriter jw, WaterPumpMonitor monitor, HttpServletRequest request) {
        final Map<String, String[]> parameterMap = request.getParameterMap();
        int lastHours = getIntParam(parameterMap, "lastHours", 24);
        int recordCount = getIntParam(parameterMap, "recordCount", 5);

        jw.addAttribute("on", monitor.isOn());
        jw.addAttribute("recCount", monitor.getRecordCount());
        jw.addAttribute("lastPeriodRecCount", monitor.getRecordCountInLastHours(lastHours));
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
