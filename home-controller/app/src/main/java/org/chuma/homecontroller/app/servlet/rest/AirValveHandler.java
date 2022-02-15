package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.controller.controller.ValveController;

public class AirValveHandler extends AbstractRestHandler<ValveController> {
    public AirValveHandler(Iterable<ValveController> valveControllers) {
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
        String action = getStringParam(requestParameters, "action");
        if (action == null) {
            int position = getMandatoryIntParam(requestParameters, "val");
            controller.setPosition(position);
        } else {
            switch (action) {
                case "open":
                    controller.open();
                    return;
                case "close":
                    controller.close();
                    return;
                default:
                    throw new IllegalArgumentException("Unknown action '" + action + "'");
            }
        }
    }
}
