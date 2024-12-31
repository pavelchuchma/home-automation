package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.extensions.external.boiler.BoilerMonitor;
import org.chuma.homecontroller.extensions.external.boiler.State;

public class BoilerHandler extends AbstractRestHandler<BoilerMonitor> {
    public BoilerHandler(Iterable<BoilerMonitor> monitors) {
        super("boiler", "boiler", monitors, (o) -> "boiler");
    }

    @Override
    void writeJsonItemValues(JsonWriter jw, BoilerMonitor monitor, HttpServletRequest request) {
        final State state = monitor.getState();
        if (state == null) {
            return;
        }

        jw.addAttribute("mode", state.getDisplayMode().toString());
        jw.addAttribute("targetTemp", state.getTargetTemp());
        jw.addAttribute("t5U", state.getTempT5U());
        jw.addAttribute("t5L", state.getTempT5L());
        jw.addAttribute("t3", state.getTempT3());
        jw.addAttribute("t4", state.getTempT4());
        jw.addAttribute("tP", state.getTempTP());
        jw.addAttribute("th", state.getTempTh());
        jw.addAttribute("on", state.isOn());
        jw.addAttribute("hot", state.isHot());
        jw.addAttribute("eHeat", state.isEHeat());
        jw.addAttribute("pump", state.isPump());
        jw.addAttribute("vacation", state.isVacation());
    }
}
