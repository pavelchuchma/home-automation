package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;

import java.util.Map;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.extensions.actor.WaterPumpMonitor;

public class WaterPumpHandler extends AbstractRestHandler<WaterPumpMonitor> {
    public WaterPumpHandler(Iterable<WaterPumpMonitor> pirStatuses) {
        super("wpump", "wpump", pirStatuses, (o) -> "wpump");
    }

    @Override
    void writeJsonItemValues(JsonWriter jw, WaterPumpMonitor mon, HttpServletRequest request) {
        final Map<String, String[]> parameterMap = request.getParameterMap();
        int lastHours = getIntParam(parameterMap, "lastHours", 24);
        int recordCount = getIntParam(parameterMap, "recordCount", 10);

        jw.addAttribute("on", mon.isOn());
        jw.addAttribute("recCount", mon.getRecordCount());
        jw.addAttribute("lastPeriodRecCount", mon.getRecordCountInLastHours(lastHours));
        try (JsonWriter arr = jw.startArrayAttribute("lastRecords")) {
            for (WaterPumpMonitor.Record r : mon.getLastRecords(recordCount)) {
                try (JsonWriter rw = arr.startObject()) {
                    rw.addAttribute("time", r.time.toString());
                    rw.addAttribute("duration", r.duration);
                }
            }
        }
    }
}
