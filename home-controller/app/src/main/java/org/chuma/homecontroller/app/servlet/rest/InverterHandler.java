package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.extensions.external.inverter.InverterManager;
import org.chuma.homecontroller.extensions.external.inverter.InverterState;
import org.chuma.homecontroller.extensions.external.inverter.impl.SolaxInverterModbusClient;
import org.chuma.homecontroller.extensions.external.inverter.impl.SolaxInverterMonitor;

public class InverterHandler extends AbstractMonitorRestHandler<SolaxInverterMonitor> {
    private final InverterManager manager;

    /**
     * @param manager null when the inverter is not managed
     */
    public InverterHandler(SolaxInverterMonitor monitor, InverterManager manager) {
        super("inverter", monitor);
        this.manager = manager;
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
        jw.addAttribute("vppMode", state.getVppMode());
        jw.addAttribute("exportAllowed", isExportAllowed());
        if (manager != null) {
            writeBatteryOwner(jw, manager.getBatteryOwner(), manager.getBatteryRefusal());
        }
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
        applyAction(monitor.getClient(), isExportAllowed(), requestParameters);
        // show the result without waiting for the next periodic refresh
        monitor.getStateSync(true);
    }

    private boolean isExportAllowed() {
        return manager != null && manager.isExportAllowed();
    }

    /**
     * Optional pgridBias: true targets the feed-in to ~+40 W (Grid), false to ~0 W (Disable). Grid is an
     * export, so it is rejected where export is not allowed.
     */
    static void applyAction(SolaxInverterModbusClient client, boolean exportAllowed,
                            Map<String, String[]> requestParameters) {
        Boolean pgridBias = getBooleanParam(requestParameters, "pgridBias");
        if (pgridBias != null) {
            if (pgridBias && !exportAllowed) {
                throw new IllegalStateException("Pgrid bias Grid rejected, export is not allowed in this deployment");
            }
            client.setPgridBias(pgridBias ? InverterState.PgridBias.Grid : InverterState.PgridBias.Disable);
        }
    }

    /**
     * Who holds the battery through a VPP mode, and who wants it and cannot get it. The refusal attributes
     * are written as null too: the UI keeps the last value of an attribute that is missing.
     */
    static void writeBatteryOwner(JsonWriter jw, String owner, InverterManager.BatteryRefusal refusal) {
        jw.addAttribute("batteryOwner", owner);
        jw.addAttribute("refusedOwner", refusal != null ? refusal.owner() : null);
        jw.addAttribute("refusal", refusal != null ? refusal.reason() : null);
    }

    /**
     * House load, grid meter power and phase voltage per phase, load computed the same way as the total one.
     */
    private static void writePhases(JsonWriter jw, InverterState state) {
        int[] grid = {state.getGrid1Power(), state.getGrid2Power(), state.getGrid3Power()};
        int[] eps = {state.getEps1Power(), state.getEps2Power(), state.getEps3Power()};
        int[] feedIn = {state.getFeedIn1Power(), state.getFeedIn2Power(), state.getFeedIn3Power()};
        double[] voltage = {state.getGrid1Voltage(), state.getGrid2Voltage(), state.getGrid3Voltage()};
        for (int i = 0; i < 3; i++) {
            jw.addAttribute("load" + (i + 1), grid[i] + eps[i] - feedIn[i]);
            jw.addAttribute("feedIn" + (i + 1), feedIn[i]);
            jw.addAttribute("voltage" + (i + 1), voltage[i]);
        }
    }
}
