package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import org.chuma.homecontroller.app.servlet.ServletAction;
import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;

public class ServletActionHandler extends AbstractRestHandler<ServletAction> {
    public ServletActionHandler(Iterable<ServletAction> servletActions) {
        super("servletActions", "servletActions", servletActions, ServletAction::getId);
    }

    @Override
    void writeJsonItemValues(JsonWriter jw, ServletAction item, HttpServletRequest request) {
        jw.addAttribute("name", item.getLabel());
    }

    @Override
    void processAction(ServletAction servletActions, Map<String, String[]> requestParameters) {
        String action = getMandatoryStringParam(requestParameters, "action");
        if ("perform".equals(action)) {
            servletActions.getAction().perform(-1);
            return;
        }
        throw new IllegalArgumentException("Unknown action '" + action + "'");
    }
}
