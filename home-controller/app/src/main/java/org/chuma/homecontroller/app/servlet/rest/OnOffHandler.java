package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class OnOffHandler extends AbstractRestHandler<IOnOffActor> {
    public OnOffHandler(Iterable<IOnOffActor> actors) {
        super("onOff", "onOff", actors, IOnOffActor::getId);
    }

    @Override
    void writeJsonItemValues(JsonWriter jw, IOnOffActor actor, HttpServletRequest request) {
        jw.addAttribute("name", actor.getLabel());
        jw.addAttribute("isOn", actor.isOn());
    }

    @Override
    void processAction(IOnOffActor actor, Map<String, String[]> requestParameters) {
        String action = getStringParam(requestParameters, "action");
        Validate.notNull(action, "Missing 'action' param");
        switch (action) {
            case "toggle":
                if (actor.isOn()) {
                    actor.switchOff(null);
                } else {
                    actor.switchOn(null);
                }
                break;
            case "on":
                actor.switchOn(null);
                break;
            case "off":
                actor.switchOff(null);
                break;
            default:
                throw new IllegalArgumentException("Unknown action '" + action + "'");
        }
    }
}
