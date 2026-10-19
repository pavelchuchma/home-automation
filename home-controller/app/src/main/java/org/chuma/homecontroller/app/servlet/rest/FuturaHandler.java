package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.extensions.external.futura.FuturaController;
import org.chuma.homecontroller.extensions.external.futura.FuturaMonitor;
import org.chuma.homecontroller.extensions.external.futura.State;
import org.chuma.homecontroller.extensions.external.futura.TimedFunction;

public class FuturaHandler extends AbstractMonitorRestHandler<FuturaMonitor> {
    public FuturaHandler(FuturaMonitor monitor) {
        super("futura", monitor);
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
        jw.addAttribute("humiAmbient", state.getHumidityAmbient());
        jw.addAttribute("humiFresh", state.getHumidityFresh());
        jw.addAttribute("humiIndoor", state.getHumidityIndoor());
        jw.addAttribute("humiWaste", state.getHumidityWaste());
        jw.addAttribute("modeFlags", state.getModeFlags());
        jw.addAttribute("errorFlags", state.getErrorFlags());
        jw.addAttribute("warningFlags", state.getWarningFlags());
        jw.addAttribute("filterWear", state.getFilterWearLevelPercent());
        jw.addAttribute("powerConsumption", state.getPowerConsumption());
        jw.addAttribute("heatRecovering", state.getHeatRecovering());
        jw.addAttribute("airFlow", state.getAirFlow());
        jw.addAttribute("fanPowerSupply", state.getFanPowerSupplyPercent());
        jw.addAttribute("fanPowerExhaust", state.getFanPowerExhaustPercent());
        jw.addAttribute("fanRpmSupply", state.getFanRpmSupply());
        jw.addAttribute("fanRpmExhaust", state.getFanRpmExhaust());
        jw.addAttribute("wallControllerCo2", state.getWallControllerCO2());
        jw.addAttribute("wallControllerTemp", state.getWallControllerTemperature());
        jw.addAttribute("wallControllerHumi", state.getWallControllerHumidity());
        jw.addAttribute("timeProgramActive", state.getTimeProgramActive());
        jw.addAttribute("tempSet", state.getPreferredTemperature());
        jw.addAttribute("humiSet", state.getPreferredHumidity());
        jw.addAttribute("bypass", state.getBypassEnabled());
        for (TimedFunction f : TimedFunction.values()) {
            jw.addAttribute(paramName(f), state.getRemainingSeconds(f));
        }
    }

    @Override
    void processAction(FuturaMonitor monitor, Map<String, String[]> requestParameters) {
        applyAction(monitor.getController(), requestParameters);
        // show the result without waiting for the next periodic refresh
        monitor.getStateSync(true);
    }

    /**
     * All parameters are optional, only the given ones are written: vent (0 off, 1-5, 6 auto), timeProg, tempSet (°C),
     * humiSet (25, 50, 75), bypass, and boost, circulation, overpressure, night, party (seconds to
     * run, 0 ends the function).
     */
    static void applyAction(FuturaController controller, Map<String, String[]> requestParameters) {
        String vent = getStringParam(requestParameters, "vent");
        if (vent != null) {
            controller.setVentilationSpeed(Integer.parseInt(vent));
        }
        Boolean timeProg = getBooleanParam(requestParameters, "timeProg");
        if (timeProg != null) {
            controller.setTimeProgramActive(timeProg);
        }
        String tempSet = getStringParam(requestParameters, "tempSet");
        if (tempSet != null) {
            controller.setPreferredTemperature(Double.parseDouble(tempSet));
        }
        String humiSet = getStringParam(requestParameters, "humiSet");
        if (humiSet != null) {
            controller.setPreferredHumidity(Integer.parseInt(humiSet));
        }
        Boolean bypass = getBooleanParam(requestParameters, "bypass");
        if (bypass != null) {
            controller.setBypassEnabled(bypass);
        }
        for (TimedFunction f : TimedFunction.values()) {
            String seconds = getStringParam(requestParameters, paramName(f));
            if (seconds != null) {
                controller.startTimedFunction(f, Integer.parseInt(seconds));
            }
        }
    }

    private static String paramName(TimedFunction f) {
        return f.name().toLowerCase(Locale.ROOT);
    }
}
