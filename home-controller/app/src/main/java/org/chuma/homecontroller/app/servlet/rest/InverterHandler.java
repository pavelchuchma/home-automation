package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.extensions.external.inverter.InverterMonitor;
import org.chuma.homecontroller.extensions.external.inverter.InverterState;

public class InverterHandler extends AbstractRestHandler<InverterMonitor> {
    public InverterHandler(Iterable<InverterMonitor> monitors) {
        super("inverter", "inverter", monitors, (o) -> "inverter");
    }

    @Override
    void writeJsonItemValues(JsonWriter jw, InverterMonitor monitor, HttpServletRequest request) {
        final InverterState state = monitor.getState();
        if (state == null) {
            return;
        }

        int pv = state.getPv1Power();
        int acPower = state.getGrid1Power() + state.getGrid2Power() + state.getGrid3Power();
        int batPower = state.getBatteryPower();
        int feedInPower = state.getFeedInPower();

        jw.addAttribute("mode", state.getMode());
        jw.addAttribute("pvPwr", pv);
        jw.addAttribute("acPwr", acPower);
        jw.addAttribute("feedInPwr", feedInPower);

        int load = acPower - feedInPower;
        jw.addAttribute("load", load);
        // just for debug
        int diff = pv - (batPower + acPower);
        jw.addAttribute("diff", diff);

        jw.addAttribute("batPwr", batPower);
        jw.addAttribute("batSoc", state.getBatterySoc());
        jw.addAttribute("yieldToday", state.getYieldToday());
        jw.addAttribute("consumedToday", state.getConsumedEnergyToday());
    }
}
