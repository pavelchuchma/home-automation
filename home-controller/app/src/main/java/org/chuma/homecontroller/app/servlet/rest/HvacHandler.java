package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.extensions.actor.HvacActor;
import org.chuma.hvaccontroller.device.FanSpeed;
import org.chuma.hvaccontroller.device.HvacDevice;
import org.chuma.hvaccontroller.device.OperatingMode;

public class HvacHandler extends AbstractRestHandler<HvacActor> {
    static final int MIN_TEMP = 16;
    static final int MAX_TEMP = 30;

    public HvacHandler(HvacActor actor) {
        super("hvac", actor);
    }

    @Override
    void writeJsonItemValues(JsonWriter jw, HvacActor a, HttpServletRequest request) {
        HvacDevice hvacDevice = a.getHvacDevice();
        if (hvacDevice == null) {
            return;
        }

        jw.addAttribute("on", hvacDevice.isRunning());
        jw.addAttribute("fanSpeed", hvacDevice.getFanSpeed().toString());
        jw.addAttribute("currentMode", hvacDevice.getCurrentMode().toString());
        jw.addAttribute("targetMode", hvacDevice.getTargetMode().toString());
        jw.addAttribute("autoMode", hvacDevice.isAutoMode());
        jw.addAttribute("quiteMode", hvacDevice.isQuiteMode());
        jw.addAttribute("sleepMode", hvacDevice.isSleepMode());
        jw.addAttribute("defrost", hvacDevice.isDefrost());
        jw.addAttribute("targetTemperature", hvacDevice.getTargetTemperature());
        jw.addAttribute("airTemperature", hvacDevice.getAirTemperature());
        jw.addAttribute("roomTemperature", hvacDevice.getRoomTemperature());
    }

    /**
     * All parameters are optional, a missing one keeps the current value: on, mode (OperatingMode), fan (FanSpeed),
     * temp (°C), sleep, quiet.
     */
    @Override
    void processAction(HvacActor hvacActor, Map<String, String[]> requestParameters) {
        String mode = getStringParam(requestParameters, "mode");
        String fan = getStringParam(requestParameters, "fan");
        int temp = getIntParam(requestParameters, "temp", 0);
        Validate.isTrue(temp == 0 || (temp >= MIN_TEMP && temp <= MAX_TEMP), "Temperature %d out of range", temp);

        boolean sent = hvacActor.set(
                getBooleanParam(requestParameters, "on"),
                (mode != null) ? OperatingMode.valueOf(mode) : OperatingMode.NONE,
                (fan != null) ? FanSpeed.valueOf(fan) : FanSpeed.NONE,
                temp,
                getBooleanParam(requestParameters, "sleep"),
                getBooleanParam(requestParameters, "quiet"),
                null);
        Validate.isTrue(sent, "HVAC unit state is not known yet, nothing sent");
    }

    private static Boolean getBooleanParam(Map<String, String[]> requestParameters, String name) {
        String val = getStringParam(requestParameters, name);
        return (val != null) ? Boolean.valueOf(val) : null;
    }
}
