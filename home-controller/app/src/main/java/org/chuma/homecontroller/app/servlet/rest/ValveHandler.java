package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.Request;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.controller.controller.ValveController;

public class ValveHandler extends AbstractRestHandler<ValveController> {
    public ValveHandler(Iterable<ValveController> valveControllers) {
        super("airValves", "airValves", valveControllers, ValveController::getId);
    }

    @Override
    void writeJsonItemValues(JsonWriter jw, ValveController item, HttpServletRequest request) {
        jw.addAttribute("name", item.getLabel());
        jw.addAttribute("pos", Double.toString(item.getPosition()));
        jw.addAttribute("act", item.getActivity().toString());
    }

    @Override
    void processAction(ValveController controller, Request baseRequest, HttpServletRequest request) {
        int position = getMandatoryIntParam(request, "val");
        controller.setPosition(position);
    }
}
