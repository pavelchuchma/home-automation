package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.extensions.external.futura.FuturaMonitor;
import org.chuma.homecontroller.extensions.external.futura.State;

public class FuturaHandler extends AbstractRestHandler<FuturaMonitor> {
    public FuturaHandler(Iterable<FuturaMonitor> monitors) {
        super("recuperation", "recuperation", monitors, (o) -> "futura");
    }

    @Override
    void writeJsonItemValues(JsonWriter jw, FuturaMonitor monitor, HttpServletRequest request) {
        final State state = monitor.getState();
        if (state == null) {
            return;
        }

        jw.addAttribute("ventSpeed", state.getVentilationSpeed());
        jw.addAttribute("airTempAmbient", state.getAirTempAmbient());
        jw.addAttribute("airTempFresh", state.getAirTempFresh());
        jw.addAttribute("airTempIndoor", state.getAirTempIndoor());
        jw.addAttribute("airTempWaste", state.getAirTempWaste());
        jw.addAttribute("filterWear", state.getFilterWearLevelPercent());
        jw.addAttribute("powerConsumption", state.getPowerConsumption());
        jw.addAttribute("heatRecovering", state.getHeatRecovering());
        jw.addAttribute("wallControllerCo2", state.getWallControllerCO2());
        jw.addAttribute("wallControllerTemp", state.getWallControllerTemperature());
        jw.addAttribute("timeProgramActive", state.getTimeProgramActive());
    }
}
