package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.extensions.external.inverter.InverterState;
import org.chuma.homecontroller.extensions.external.inverter.impl.SolaxInverterModbusClient;
import org.chuma.homecontroller.extensions.external.inverter.impl.SolaxInverterMonitor;

public class InverterHandler extends AbstractMonitorRestHandler<SolaxInverterMonitor> {
    public InverterHandler(SolaxInverterMonitor monitor) {
        super("inverter", monitor);
    }

    @Override
    void writeJsonItemValues(JsonWriter jw, SolaxInverterMonitor monitor, HttpServletRequest request) {
        final InverterState state = monitor.getState();
        if (state == null) {
            return;
        }

        int pv = state.getPv1Power();
        int acPower = state.getGridPower();
        int epsPower = state.getEpsPower();
        int batPower = state.getBatteryPower();
        int feedInPower = state.getFeedInPower();

        jw.addAttribute("mode", state.getMode().toString());
        jw.addAttribute("batMode", state.getBatteryMode().toString());
        jw.addAttribute("manualMode", state.getManualMode().toString());
        jw.addAttribute("pgridBias", state.getPgridBias().toString());
        jw.addAttribute("pvPwr", pv);
        jw.addAttribute("acPwr", acPower);
        jw.addAttribute("epsPwr", epsPower);
        jw.addAttribute("feedInPwr", feedInPower);

        int load = acPower + epsPower - feedInPower;
        jw.addAttribute("load", load);
        writePhases(jw, state);
        // just for debug
        int diff = pv - (batPower + acPower);
        jw.addAttribute("diff", diff);

        jw.addAttribute("batPwr", batPower);
        jw.addAttribute("batSoc", state.getBatterySoc());
        jw.addAttribute("yieldToday", state.getYieldToday());
        jw.addAttribute("pvYieldToday", state.getPvYieldToday());
        jw.addAttribute("consumedToday", state.getConsumedEnergyToday());
        jw.addAttribute("feedInToday", state.getFeedInEnergyToday());
    }

    @Override
    void processAction(SolaxInverterMonitor monitor, Map<String, String[]> requestParameters) {
        applyAction(monitor.getClient(), requestParameters);
        // show the result without waiting for the next periodic refresh
        monitor.getStateSync(true);
    }

    /**
     * Optional pgridBias: true targets the feed-in to ~+40 W (Grid), false to ~0 W (Disable).
     */
    static void applyAction(SolaxInverterModbusClient client, Map<String, String[]> requestParameters) {
        Boolean pgridBias = getBooleanParam(requestParameters, "pgridBias");
        if (pgridBias != null) {
            client.setPgridBias(pgridBias ? InverterState.PgridBias.Grid : InverterState.PgridBias.Disable);
        }
    }

    /**
     * House load and grid meter power per phase, load computed the same way as the total one.
     */
    private static void writePhases(JsonWriter jw, InverterState state) {
        int[] grid = {state.getGrid1Power(), state.getGrid2Power(), state.getGrid3Power()};
        int[] eps = {state.getEps1Power(), state.getEps2Power(), state.getEps3Power()};
        int[] feedIn = {state.getFeedIn1Power(), state.getFeedIn2Power(), state.getFeedIn3Power()};
        for (int i = 0; i < 3; i++) {
            jw.addAttribute("load" + (i + 1), grid[i] + eps[i] - feedIn[i]);
            jw.addAttribute("feedIn" + (i + 1), feedIn[i]);
        }
    }
}
