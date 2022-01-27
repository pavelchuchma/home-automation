package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.controller.controller.ValveController;

public class ValveHandler extends AbstractRestHandler<ValveController> {
    public ValveHandler(Iterable<ValveController> valveControllers) {
        super("airValves", "airValves", valveControllers, ValveController::getId);
    }

    @Override
    void writeJsonItemValues(JsonWriter jw, ValveController item, HttpServletRequest request) {
        jw.addAttribute("name", item.getLabel());
        jw.addAttribute("pos", item.getPosition());
        jw.addAttribute("act", item.getActivity().toString());
    }

    @Override
    void processAction(ValveController controller, Map<String, String[]> requestParameters) {
        int position = getMandatoryIntParam(requestParameters, "val");
        controller.setPosition(position);
    }
}
