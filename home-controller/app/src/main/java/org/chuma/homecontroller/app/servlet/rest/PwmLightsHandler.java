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
        jw.addAttribute("val", actor.getPwmValue());
        jw.addAttribute("maxVal", actor.getMaxPwmValue());
        jw.addAttribute("curr", actor.getOutputCurrent());
    }

    @Override
    void processAction(PwmActor pwmActor, Map<String, String[]> requestParameters) {
        String action = getStringParam(requestParameters, "action");
        int currentVal = pwmActor.getPwmValuePercent();
        int newVal;
        if (action == null) {
            newVal = getMandatoryIntParam(requestParameters, "val");
        } else {
            switch (action) {
                case "toggle":
                    newVal = (pwmActor.isOn()) ? 0 : 75;
                    break;
                case "plus":
                    newVal = (pwmActor.isOn()) ? Math.min(currentVal + 15, 100) : 66;
                    break;
                case "minus":
                    newVal = (pwmActor.isOn()) ? Math.max(currentVal - 15, 0) : 1;
                    break;
                case "full":
                    newVal = 100;
                    break;
                case "off":
                    newVal = 0;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown action '" + action + "'");
            }
        }
        pwmActor.setValue(newVal, this);
    }
}
