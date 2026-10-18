package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.app.servlet.Handler;
import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.base.utils.Options;

/**
 * Reads and updates selected options as JSON, used by the settings dialogs of the floor plan widgets.
 * <ul>
 * <li>{@code GET /rest/options?keys=a,b} returns {@code {"a": "...", "b": "..."}}
 * <li>{@code POST /rest/options/save} with form parameters {@code key=value} updates and saves them
 * </ul>
 */
public class OptionsHandler implements Handler {
    private static final Logger log = LoggerFactory.getLogger(OptionsHandler.class);
    private static final String ROOT_PATH = "/rest/options";
    private static final String SAVE_PATH = ROOT_PATH + "/save";
    private final Options options;

    public OptionsHandler(Options options) {
        this.options = options;
    }

    @Override
    public String getPath() {
        return ROOT_PATH;
    }

    @Override
    public void handle(String target, Request request, HttpServletResponse response) throws IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "*");
        try {
            if (target.startsWith(SAVE_PATH)) {
                Map<String, String> values = new LinkedHashMap<>();
                request.getParameterMap().forEach((k, v) -> values.put(k, v[0]));
                update(options, values);
                options.save();
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                String keys = AbstractRestHandler.getMandatoryStringParam(request.getParameterMap(), "keys");
                JsonWriter writer = new JsonWriter(true);
                try (JsonWriter root = writer.startObject()) {
                    for (Map.Entry<String, String> e : read(options, keys.split(",")).entrySet()) {
                        root.addAttribute(e.getKey(), escape(e.getValue()));
                    }
                }
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().print(writer);
                response.setStatus(HttpServletResponse.SC_OK);
            }
        } catch (RuntimeException e) {
            log.warn("Options request '{}' failed: {}", target, e.toString());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain;charset=utf-8");
            response.getWriter().print(e.getMessage() != null ? e.getMessage() : e.toString());
        }
        request.setHandled(true);
    }

    /**
     * Returns values of the keys, an empty value as an empty string.
     */
    static Map<String, String> read(Options options, String[] keys) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : keys) {
            if (!options.getNames().contains(key)) {
                throw new IllegalArgumentException("Property '" + key + "' is unknown");
            }
            String value = options.get(key);
            result.put(key, value != null ? value : "");
        }
        return result;
    }

    /**
     * Puts all values or none: if a listener rejects a value, the already changed ones are restored.
     */
    static void update(Options options, Map<String, String> values) {
        Map<String, String> previous = new LinkedHashMap<>();
        try {
            for (Map.Entry<String, String> e : values.entrySet()) {
                String old = options.get(e.getKey());
                options.put(e.getKey(), e.getValue());
                previous.put(e.getKey(), old);
            }
        } catch (RuntimeException e) {
            previous.forEach(options::put);
            throw e;
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
