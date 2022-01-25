package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;

public class AllStatusHandler extends AbstractRestHandler<StatusHandler> {
    public AllStatusHandler(Iterable<StatusHandler> handlers) {
        super("all", "all", handlers, StatusHandler::getStatusJsonArrayName);
    }

    @Override
    public void writeStatusJson(JsonWriter writer, HttpServletRequest request) {
        for (Map.Entry<String, StatusHandler> entry : itemMap.entrySet()) {
            entry.getValue().writeStatusJson(writer, request);
        }
    }
}
