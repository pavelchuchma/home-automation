package org.chuma.homecontroller.extensions.external.inverter.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.Jsonable;
import com.github.cliftonlabs.json_simple.Jsoner;

public class HttpJsonClient {
    private final String baseUrl;
    private final int readTimeoutSec;

    public HttpJsonClient(String baseUrl, int readTimeoutSec) {
        this.baseUrl = baseUrl;
        this.readTimeoutSec = readTimeoutSec;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public <R extends Jsonable> R doPost(String path, String formPayload) {
        return doPost(path, formPayload, null);
    }

    public <R extends Jsonable> R doPost(String path, String formPayload, String token) {
        return doCall("POST", path, conn ->
        {
            byte[] postDataBytes = (formPayload).getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if (token != null) {
                conn.setRequestProperty("token", token);
            }
            conn.setDoOutput(true);
            try {
                conn.getOutputStream().write(postDataBytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <R extends Jsonable> R doGet(String path) {
        return doCall("GET", path, null);
    }

    public <R extends Jsonable> R doCall(String verb, String path, Consumer<HttpURLConnection> requestBuilder) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection)new URL(baseUrl + path).openConnection();
            conn.setRequestMethod(verb);
            conn.setReadTimeout(readTimeoutSec * 1000);

            if (requestBuilder != null) {
                requestBuilder.accept(conn);
            }

            Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            //noinspection unchecked
            return (R)Jsoner.deserialize(in);
        } catch (JsonException | IOException e) {
            throw new RuntimeException("Failed to call " + verb + " to " + baseUrl + path, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
