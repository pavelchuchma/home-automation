package org.chuma.homecontroller.extensions.metrics;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * POSTs line protocol to the InfluxDB 2.x write API.
 */
class InfluxHttpSender implements MetricsReporter.Sender {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final URI uri;
    private final String token;
    private final HttpClient client;

    InfluxHttpSender(String baseUrl, String org, String bucket, String token) {
        if (org == null || bucket == null || token == null) {
            throw new IllegalArgumentException("InfluxDB org, bucket and token must be set");
        }
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        uri = URI.create(base + "/api/v2/write?org=" + encode(org) + "&bucket=" + encode(bucket) + "&precision=ms");
        this.token = token;
        client = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    }

    @Override
    public void send(String body) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Token " + token)
                .header("Content-Type", "text/plain; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while sending metrics", e);
        }
        if (response.statusCode() != 204) {
            String detail = response.body();
            if (detail.length() > 300) {
                detail = detail.substring(0, 300) + "...";
            }
            throw new IOException("HTTP " + response.statusCode() + ": " + detail);
        }
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return uri.getScheme() + "://" + uri.getAuthority() + uri.getPath() + "?" + uri.getQuery();
    }
}
