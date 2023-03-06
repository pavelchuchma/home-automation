package org.chuma.homecontroller.extensions.external.inverter.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsonable;
import com.github.cliftonlabs.json_simple.Jsoner;
import org.apache.commons.lang3.Validate;

public class HttpJsonClient {
    private final String baseUrl;
    private final int readTimeoutSec;

    public HttpJsonClient(String baseUrl, int readTimeoutSec) {
        this.baseUrl = baseUrl;
        this.readTimeoutSec = readTimeoutSec;
    }

    public JsonObject doPostAndVerify(String path, String formPayload) {
        JsonObject response = doPost(path, formPayload);
        Validate.isTrue(Boolean.TRUE.equals(response.get("success")),
                "Posting '%s' to '%s' failed: %s", formPayload, baseUrl + path, response.get("exception"));
        return response;
    }

    public <R extends Jsonable> R doPost(String path, String formPayload) {
        return doPost(path, formPayload, null);
    }

    public <R extends Jsonable> R doPost(String path, String formPayload, String token) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection)new URL(baseUrl + path).openConnection();
            conn.setRequestMethod("POST");
            conn.setReadTimeout(readTimeoutSec * 1000);
            byte[] postDataBytes = (formPayload).getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if (token != null) {
                conn.setRequestProperty("token", token);
            }
            conn.setDoOutput(true);
            conn.getOutputStream().write(postDataBytes);
            Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            //noinspection unchecked
            return (R)Jsoner.deserialize(in);
        } catch (JsonException | IOException e) {
            throw new RuntimeException("Failed to call POST to " + baseUrl + path, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
