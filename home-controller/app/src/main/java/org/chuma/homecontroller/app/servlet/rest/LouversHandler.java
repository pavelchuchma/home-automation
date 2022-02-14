package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.controller.controller.Activity;
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
        String action = getMandatoryStringParam(requestParameters, "action");
        switch (action) {
            case "up":
                doOrStopIfAlreadyDoing(controller, Activity.movingUp, controller::up);
                return;
            case "blind":
                doOrStopIfAlreadyDoing(controller, Activity.movingDown, controller::blind);
                return;
            case "outshine":
                int offset = getIntParam(requestParameters, "off", 0);
                controller.outshine(offset);
                return;
            default:
                throw new RuntimeException("Unknown action '" + action + "'");
        }
    }

    private void doOrStopIfAlreadyDoing(LouversController controller, Activity activity, Runnable action) {
        if (controller.getActivity() == activity) {
            controller.stop();
        } else {
            action.run();
        }
    }
}
