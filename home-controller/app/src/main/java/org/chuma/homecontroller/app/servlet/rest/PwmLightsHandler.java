package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.controller.actor.AbstractActor;
import org.chuma.homecontroller.controller.actor.LddActor;

public class PwmLightsHandler extends AbstractRestHandler<LddActor> {
    public PwmLightsHandler(Iterable<LddActor> actors) {
        super("pwmLights", "pwmLights", actors, AbstractActor::getId);
    }

    @Override
    void writeJsonItemValues(JsonWriter jw, LddActor actor, HttpServletRequest request) {
        jw.addAttribute("name", actor.getLabel());
        jw.addAttribute("val", actor.getValue());
        jw.addAttribute("pwmVal", actor.getCurrentPwmValue());
        jw.addAttribute("maxPwmVal", actor.getLddOutput().getMaxPwmValue());
        jw.addAttribute("curr", actor.getOutputCurrent());
    }

    @Override
    void processAction(LddActor lddActor, Map<String, String[]> requestParameters) {
        String action = getStringParam(requestParameters, "action");
        if (action == null) {
            double val = getMandatoryDoubleParam(requestParameters, "val");
            lddActor.switchOn(val, null);
        } else {
            switch (action) {
                case "toggle":
                    if (lddActor.isOn()) {
                        lddActor.switchOff(null);
                    } else {
                        lddActor.switchOn(0.75, null);
                    }
                    break;
                case "plus":
                    if (lddActor.isOn()) {
                        lddActor.increasePwm(.15, null);
                    } else {
                        lddActor.switchOn(0.66, null);
                    }
                    break;
                case "minus":
                    if (lddActor.isOn()) {
                        lddActor.decreasePwm(.15, null);
                    } else {
                        lddActor.switchOn(0.01, null);
                    }
                    break;
                case "full":
                    lddActor.switchOn(null);
                    break;
                case "off":
                    lddActor.switchOff(null);
                    break;
                case "increase":
                    double val = getMandatoryDoubleParam(requestParameters, "val");
                    lddActor.increasePwm(val, null);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown action '" + action + "'");
            }
        }
    }
}
