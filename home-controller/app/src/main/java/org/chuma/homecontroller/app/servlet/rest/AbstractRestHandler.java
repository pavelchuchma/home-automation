package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import org.apache.commons.lang3.Validate;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.app.servlet.Handler;
import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;

public abstract class AbstractRestHandler<T> implements Handler, StatusHandler {
    private static final Logger log = LoggerFactory.getLogger(AbstractRestHandler.class);
    final String rootPath;
    final String statusPath;
    final String actionPath;
    final Map<String, T> itemMap;
    final String statusJsonArrayName;

    public AbstractRestHandler(String subPath, String statusJsonArrayName, Iterable<T> items, Function<T, String> getId) {
        this.rootPath = "/rest/" + subPath;
        statusPath = rootPath + "/status";
        actionPath = rootPath + "/action";
        this.statusJsonArrayName = statusJsonArrayName;
        this.itemMap = buildIdMap(items, getId);
    }

    private static <T> Map<String, T> buildIdMap(Iterable<T> items, Function<T, String> getId) {
        Map<String, T> itemMap = new TreeMap<>();
        for (T i : items) {
            String id = getId.apply(i);
            if (itemMap.put(id, i) != null) {
                throw new RuntimeException("Item with id '" + id + "' is not unique");
            }
        }
        return itemMap;
    }

    static String getMandatoryStringParam(Map<String, String[]> requestParams, String name) {
        String val = getStringParam(requestParams, name);
        if (val == null) {
            throw new IllegalArgumentException("Mandatory parameter '" + name + "' is missing");
        }
        return val;
    }

    static String getStringParam(Map<String, String[]> requestParams, String name) {
        String[] values = requestParams.get(name);
        if (values == null || values.length == 0) {
            return null;
        }
        return values[0];
    }

    static int getMandatoryIntParam(Map<String, String[]> requestParams, String name) {
        return Integer.parseInt(getMandatoryStringParam(requestParams, name));
    }

    static int getIntParam(Map<String, String[]> requestParams, String name, int defaultValue) {
        String val = getStringParam(requestParams, name);
        if (val == null) {
            return defaultValue;
        }
        return Integer.parseInt(val);
    }

    @Override
    public String getRootPath() {
        return rootPath;
    }

    @Override
    public String getStatusJsonArrayName() {
        return statusJsonArrayName;
    }

    @Override
    public void handle(String target, Request request, HttpServletResponse response) throws IOException {
        if (target.startsWith(statusPath)) {
            log.debug("writeStatus: {}", this.getClass().getSimpleName());
            JsonWriter writer = new JsonWriter(true);
            try (JsonWriter root = writer.startObject()) {
                writeStatusJson(root, request);
            }

            response.setContentType("application/json;charset=utf-8");
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.getWriter().print(writer);
            response.setStatus(HttpServletResponse.SC_OK);

            request.setHandled(true);
        } else if (target.startsWith(actionPath)) {
            final Map<String, String[]> requestParameters = request.getParameterMap();
            T item = getItemById(requestParameters);
            new Thread(() -> {
                try {
                    processAction(item, requestParameters);
                } catch (Exception e) {
                    log.error("Failed to handle action: " + target, e);
                }
            }).start();
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
        }
    }

    public void writeStatusJson(JsonWriter writer, HttpServletRequest request) {
        try (JsonWriter arrayWriter = writer.startArrayAttribute(statusJsonArrayName)) {
            for (Map.Entry<String, T> entry : itemMap.entrySet()) {
                try (JsonWriter objectWriter = arrayWriter.startObject()) {
                    objectWriter.addAttribute("id", entry.getKey());
                    writeJsonItemValues(objectWriter, entry.getValue(), request);
                }
            }
        }
    }

    void writeJsonItemValues(JsonWriter jw, T item, HttpServletRequest request) {
    }

    void processAction(T instance, Map<String, String[]> requestParameters) {
    }

    T getItemById(Map<String, String[]> requestParams) {
        String id = getMandatoryStringParam(requestParams, "id");
        T item = itemMap.get(id);
        Validate.notNull(item, "no item with id '" + id + "' found");
        return item;
    }
}
