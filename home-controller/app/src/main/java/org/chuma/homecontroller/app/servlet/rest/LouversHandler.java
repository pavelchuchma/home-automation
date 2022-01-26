package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;

import java.util.Map;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.controller.controller.LouversController;

public class LouversHandler extends AbstractRestHandler<LouversController> {
    public LouversHandler(Iterable<LouversController> louversControllers) {
        super("louvers", "louvers", louversControllers, LouversController::getId);
    }

    @Override
    void writeJsonItemValues(JsonWriter jw, LouversController item, HttpServletRequest request) {
        jw.addAttribute("name", item.getLabel());
        jw.addAttribute("pos", item.getPosition());
        jw.addAttribute("off", item.getOffset());
        jw.addAttribute("act", item.getActivity().toString());
    }

    @Override
    void processAction(LouversController controller, Map<String, String[]> requestParameters) {
        int position = getMandatoryIntParam(requestParameters, "pos");
        int offset = getMandatoryIntParam(requestParameters, "off");
        if (position == 0) {
            controller.up();
        } else if (position == 100) {
            controller.outshine(offset);
        }
    }
}
