package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.apache.commons.lang3.Validate;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.app.servlet.Handler;
import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;

public abstract class AbstractRestHandler<T> implements Handler, StatusHandler {
    private static final Logger log = LoggerFactory.getLogger(AbstractRestHandler.class);
    private final ExecutorService executor = Executors.newCachedThreadPool();
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

        itemMap = new TreeMap<>();
        for (T i : items) {
            addMapItem(i, getId);
        }
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

    public static int getMandatoryIntParam(Map<String, String[]> requestParams, String name) {
        return Integer.parseInt(getMandatoryStringParam(requestParams, name));
    }

    public static double getMandatoryDoubleParam(Map<String, String[]> requestParams, String name) {
        return Double.parseDouble(getMandatoryStringParam(requestParams, name));
    }

    static int getIntParam(Map<String, String[]> requestParams, String name, int defaultValue) {
        String val = getStringParam(requestParams, name);
        if (val == null) {
            return defaultValue;
        }
        return Integer.parseInt(val);
    }

    /**
     * Adds single item with unique ID to the internal map.
     */
    void addMapItem(T item, Function<T, String> getId) {
        String id = getId.apply(item);
        if (itemMap.put(id, item) != null) {
            throw new RuntimeException("Item with id '" + id + "' is not unique");
        }
    }

    @Override
    public String getPath() {
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
            T item = getItemById(getMandatoryStringParam(requestParameters, "id"));
            executor.execute(() -> {
                try {
                    processAction(item, requestParameters);
                } catch (Exception e) {
                    log.error("Failed to handle action: " + target, e);
                }
            });
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
        }
    }

    public void writeStatusJson(JsonWriter writer, HttpServletRequest request) {
        try (JsonWriter arrayWriter = writer.startArrayAttribute(statusJsonArrayName)) {
            String id = getStringParam(request.getParameterMap(), "id");
            if (id != null) {
                // returning only item requested by id param
                writeStatusJsonItem(request, arrayWriter, id, getItemById(id));
            } else {
                // returning complete collection
                for (Map.Entry<String, T> entry : itemMap.entrySet()) {
                    writeStatusJsonItem(request, arrayWriter, entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private void writeStatusJsonItem(HttpServletRequest request, JsonWriter arrayWriter, String id, T item) {
        try (JsonWriter objectWriter = arrayWriter.startObject()) {
            writeIdImpl(id, objectWriter);
            writeJsonItemValues(objectWriter, item, request);
        }
    }

    void writeIdImpl(String id, JsonWriter objectWriter) {
        objectWriter.addAttribute("id", id);
    }

    void writeJsonItemValues(JsonWriter jw, T item, HttpServletRequest request) {
    }

    void processAction(T instance, Map<String, String[]> requestParameters) throws Exception {
    }

    T getItemById(String id) {
        T item = itemMap.get(id);
        Validate.isTrue(item != null, "no item with id '" + id + "' found");
        return item;
    }
}
