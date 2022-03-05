package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.controller.actor.AbstractActor;
import org.chuma.homecontroller.controller.actor.PwmActor;

public class PwmLightsHandler extends AbstractRestHandler<PwmActor> {
    public PwmLightsHandler(Iterable<PwmActor> actors) {
        super("pwmLights", "pwmLights", actors, AbstractActor::getId);
    }

    @Override
    void writeJsonItemValues(JsonWriter jw, PwmActor actor, HttpServletRequest request) {
        jw.addAttribute("name", actor.getLabel());
        jw.addAttribute("val", actor.getValue());
        jw.addAttribute("pwmVal", actor.getCurrentPwmValue());
        jw.addAttribute("maxPwmVal", actor.getMaxPwmValue());
        jw.addAttribute("curr", actor.getOutputCurrent());
    }

    @Override
    void processAction(PwmActor pwmActor, Map<String, String[]> requestParameters) {
        String action = getStringParam(requestParameters, "action");
        if (action == null) {
            double val = getMandatoryDoubleParam(requestParameters, "val");
            pwmActor.switchOn(val, null);
        } else {
            switch (action) {
                case "toggle":
                    if (pwmActor.isOn()) {
                        pwmActor.switchOff(null);
                    } else {
                        pwmActor.switchOn(0.75, null);
                    }
                    break;
                case "plus":
                    if (pwmActor.isOn()) {
                        pwmActor.increasePwm(.15, null);
                    } else {
                        pwmActor.switchOn(0.66, null);
                    }
                    break;
                case "minus":
                    if (pwmActor.isOn()) {
                        pwmActor.decreasePwm(.15, null);
                    } else {
                        pwmActor.switchOn(0.01, null);
                    }
                    break;
                case "full":
                    pwmActor.switchOn(null);
                    break;
                case "off":
                    pwmActor.switchOff(null);
                    break;
                case "increase":
                    double val = getMandatoryDoubleParam(requestParameters, "val");
                    pwmActor.increasePwm(val, null);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown action '" + action + "'");
            }
        }
    }
}
