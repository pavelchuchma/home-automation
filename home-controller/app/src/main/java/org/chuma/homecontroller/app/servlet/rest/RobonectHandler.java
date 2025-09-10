package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.extensions.external.robonect.RobonectMonitor;
import org.chuma.homecontroller.extensions.external.robonect.State;
import org.chuma.homecontroller.extensions.external.robonect.client.model.Gps;
import org.chuma.homecontroller.extensions.external.robonect.client.model.Status;
import org.chuma.homecontroller.extensions.external.robonect.client.model.WeatherInfo;

public class RobonectHandler extends AbstractRestHandler<RobonectMonitor> {
    public RobonectHandler(Iterable<RobonectMonitor> monitors) {
        super("robonect", "robonect", monitors, (o) -> "robonect");
    }

    @Override
    void writeJsonItemValues(JsonWriter jw, RobonectMonitor monitor, HttpServletRequest request) {
        final State state = monitor.getState();
        if (state == null) {
            return;
        }

        Status status = state.status();
        if (status == null) {
            return;
        }
        jw.addAttribute("mode", status.getMode().toString());
        jw.addAttribute("status", status.getStatus().toString());
        jw.addAttribute("battery", status.getBattery());
        jw.addAttribute("stopped", status.isStopped());
        jw.addAttribute("home", status.isHome());
        jw.addAttribute("now", monitor.getTimestamp());
        jw.addAttribute("timer", state.timer().getStatus().toString());
        boolean isWeatherBreak = state.weather() != null && state.weather().isBreak();
        jw.addAttribute("weatherBreak", isWeatherBreak);
        if (isWeatherBreak) {
            WeatherInfo.Weather.WeatherCondition condition = state.weather().condition();
            if (condition != null) {
                jw.addAttribute("weatherBreakReason",
                        (condition.toorainy()) ? "toorainy"
                                : (condition.toocold()) ? "toocold"
                                : (condition.toowarm()) ? "toowarm"
                                : (condition.toodry()) ? "toodry"
                                : (condition.toowet()) ? "toowet"
                                : "unknown");
            }
        }

        Gps gps = state.gps();
        if (gps != null) {
            jw.addAttribute("satellites", gps.getSatellites());
        }
        final Map<String, String[]> parameterMap = request.getParameterMap();
        long fromTimestamp = getLongParam(parameterMap, "robonectFromTime", -5 * 60);

        RobonectMonitor.GpsHistoryEntry[] gpsHistory = monitor.getGpsHistory(fromTimestamp);
        if (gpsHistory != null && gpsHistory.length > 0) {
            try (JsonWriter arr = jw.startArrayAttribute("gpsHistory")) {
                for (RobonectMonitor.GpsHistoryEntry r : gpsHistory) {
                    try (JsonWriter rw = arr.startObject()) {
                        rw.addAttribute("lat", r.latitude());
                        rw.addAttribute("lon", r.longitude());
                        rw.addAttribute("time", r.timestamp());
                    }
                }
            }
        }
    }
}

