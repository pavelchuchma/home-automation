package org.chuma.homecontroller.extensions.external.watertank;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Talks to the water-level-meter ESP's {@code /api/get} endpoint. The ESP runs a blocking
 * ~100-sample measurement sequence every 5 minutes and can be unresponsive to HTTP requests for
 * several seconds while it does so, so a single attempt uses a generous timeout, and one retry is
 * made after a 10s pause to let the ESP finish its current cycle.
 */
public class WaterTankClient {
    private static final Logger log = LoggerFactory.getLogger(WaterTankClient.class.getName());
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 8_000;
    private static final int RETRY_DELAY_MS = 10_000;

    private final String url;

    public WaterTankClient(String host) {
        this.url = "http://" + host + "/api/get";
    }

    public Reading readLevel() {
        try {
            return doRead();
        } catch (Exception e) {
            log.warn("First attempt to read water tank level failed, retrying in {} ms", RETRY_DELAY_MS, e);
        }

        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            return doRead();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read water tank level from " + url, e);
        }
    }

    private Reading doRead() throws IOException, JsonException {
        HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);

            try (Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                JsonObject response = (JsonObject)Jsoner.deserialize(in);
                int distanceMm = ((Number)response.get("distance")).intValue();
                String status = (String)response.get("status");
                int ageSec = ((Number)response.get("ageSec")).intValue();
                int samples = ((Number)response.get("samples")).intValue();
                int validSamples = ((Number)response.get("validSamples")).intValue();
                return new Reading(distanceMm, status, ageSec, samples, validSamples);
            }
        } finally {
            conn.disconnect();
        }
    }

    public static class Reading {
        public final int distanceMm;
        public final String status;
        public final int ageSec;
        public final int samples;
        public final int validSamples;

        public Reading(int distanceMm, String status, int ageSec, int samples, int validSamples) {
            this.distanceMm = distanceMm;
            this.status = status;
            this.ageSec = ageSec;
            this.samples = samples;
            this.validSamples = validSamples;
        }
    }
}
