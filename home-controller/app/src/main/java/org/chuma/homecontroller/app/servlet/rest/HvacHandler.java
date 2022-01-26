package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;

import java.util.Map;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.extensions.actor.HvacActor;
import org.chuma.hvaccontroller.device.HvacDevice;

public class HvacHandler extends AbstractRestHandler<HvacActor> {
    public HvacHandler(Iterable<HvacActor> hvacActors) {
        super("hvac", "hvac", hvacActors, HvacActor::getId);
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
        jw.addAttribute("air2Temperature", hvacDevice.getAir2Temperature());
        jw.addAttribute("roomTemperature", hvacDevice.getRoomTemperature());
        jw.addAttribute("unitTemperature", hvacDevice.getUnitTemperature());
    }

    @Override
    void processAction(HvacActor hvacActor, Map<String, String[]> requestParameters) {
        if ("true".equals(getMandatoryStringParam(requestParameters, "on"))) {
            hvacActor.switchOn(100, null);
        } else {
            hvacActor.switchOff(null);
        }
    }
}
