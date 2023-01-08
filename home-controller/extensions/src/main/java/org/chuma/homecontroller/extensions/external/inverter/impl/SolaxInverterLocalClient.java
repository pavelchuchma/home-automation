package org.chuma.homecontroller.extensions.external.inverter.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import org.chuma.homecontroller.extensions.external.inverter.impl.SolaxInverterState;

/**
 * Client for local API of "Solax Pocket wi-fi V3.0" connected to "Solax X3-Hybrid G4 Inverter"
 */
public class SolaxInverterLocalClient {
    private final String url;
    private final String password;

    public SolaxInverterLocalClient(String url, String password) {
        this.url = url;
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public SolaxInverterState getState() throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection)new URL(url).openConnection();
            conn.setRequestMethod("POST");
            byte[] postDataBytes = ("optType=ReadRealTimeData&pwd=" + password).getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            conn.setDoOutput(true);
            conn.getOutputStream().write(postDataBytes);
            Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            JsonObject json = (JsonObject)Jsoner.deserialize(in);
            return new SolaxInverterState(json);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
