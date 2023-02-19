package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.controller.action.AbstractSwitchOnActionWithTimer;
import org.chuma.homecontroller.controller.action.SwitchOnActionWithTimer;
import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class OnOffHandler extends AbstractRestHandler<IOnOffActor> {
    public OnOffHandler(Iterable<IOnOffActor> actors) {
        super("onOff", "onOff", actors, IOnOffActor::getId);
    }

    @Override
    void writeJsonItemValues(JsonWriter jw, IOnOffActor actor, HttpServletRequest request) {
        jw.addAttribute("name", actor.getLabel());
        jw.addAttribute("val", (actor.isOn()) ? 1 : 0);
    }

    @Override
    void processAction(IOnOffActor actor, Map<String, String[]> requestParameters) {
        String action = getStringParam(requestParameters, "action");
        Validate.notNull(action, "Missing 'action' param");
        int timeout = getIntParam(requestParameters, "timeout", 0);
        switch (action) {
            case "toggle":
                if (actor.isOn()) {
                    actor.switchOff();
                } else {
                    switchOnImpl(actor, timeout);
                }
                break;
            case "on":
                switchOnImpl(actor, timeout);
                break;
            case "off":
                actor.switchOff();
                break;
            default:
                throw new IllegalArgumentException("Unknown action '" + action + "'");
        }
    }

    void switchOnImpl(IOnOffActor actor, int timeout) {
        if (timeout <= 0) {
            actor.switchOn();
        } else {
            SwitchOnActionWithTimer action = new SwitchOnActionWithTimer(actor, timeout,
                    AbstractSwitchOnActionWithTimer.Priority.HIGH, null);
            action.perform(0);
        }
    }
}
