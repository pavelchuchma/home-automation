package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static org.chuma.homecontroller.app.servlet.Servlet.currentValueFormatter;

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
        int val = getMandatoryIntParam(requestParameters, "val");
        pwmActor.setValue(val, this);
    }
}
