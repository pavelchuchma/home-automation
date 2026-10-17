package org.chuma.homecontroller.extensions.metrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.utils.Options;

/**
 * Samples all registered {@link MetricSource}s once per period and POSTs the batch, together with any events
 * queued by {@link #event(Consumer)} in the meantime, to InfluxDB in a single request. A failed POST is logged
 * and the batch dropped; nothing is buffered to disk.
 */
public class MetricsReporter {
    public static final String CFG_URL = "influx.url";
    public static final String CFG_ORG = "influx.org";
    public static final String CFG_BUCKET = "influx.bucket";
    public static final String CFG_TOKEN = "influx.token";
    public static final String CFG_PREFIX = "influx.measurement.prefix";

    private static final long PERIOD_MS = 60_000;
    private static final Logger log = LoggerFactory.getLogger(MetricsReporter.class.getName());

    interface Sender {
        void send(String body) throws IOException;
    }

    private final String prefix;
    private final Sender sender;
    private final List<MetricSource> sources = new ArrayList<>();
    private final StringBuilder pendingEvents = new StringBuilder();
    private ScheduledExecutorService executor;
    private boolean failing;

    MetricsReporter(String prefix, Sender sender) {
        this.prefix = prefix;
        this.sender = sender;
    }

    /**
     * @return reporter configured from options, or null when {@code influx.url} is empty (reporting disabled)
     */
    public static MetricsReporter fromOptions(Options options) {
        String url = options.get(CFG_URL);
        if (url == null) {
            log.info("Metrics reporting disabled, {} not set", CFG_URL);
            return null;
        }
        String prefix = options.get(CFG_PREFIX);
        if (prefix == null) {
            throw new IllegalArgumentException(CFG_PREFIX + " must not be empty");
        }
        InfluxHttpSender sender = new InfluxHttpSender(url, options.get(CFG_ORG), options.get(CFG_BUCKET), options.get(CFG_TOKEN));
        log.info("Metrics reporting to {} with measurement prefix '{}'", sender, prefix);
        return new MetricsReporter(prefix, sender);
    }

    public String getPrefix() {
        return prefix;
    }

    public synchronized void addSource(MetricSource source) {
        sources.add(source);
    }

    /**
     * Queues lines written by {@code writer} for the next flush. Safe to call from any thread.
     */
    public void event(Consumer<LineProtocolWriter> writer) {
        LineProtocolWriter w = new LineProtocolWriter(prefix);
        writer.accept(w);
        if (w.isEmpty()) {
            return;
        }
        synchronized (pendingEvents) {
            pendingEvents.append(w);
        }
    }

    /**
     * Starts the periodic flush, aligned to whole minutes.
     */
    public synchronized void start() {
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MetricsReporter");
            t.setDaemon(true);
            return t;
        });
        long now = System.currentTimeMillis();
        long initialDelay = PERIOD_MS - now % PERIOD_MS;
        executor.scheduleAtFixedRate(() -> tick(System.currentTimeMillis()), initialDelay, PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    void tick(long nowMs) {
        LineProtocolWriter writer = new LineProtocolWriter(prefix);
        List<MetricSource> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(sources);
        }
        for (MetricSource source : snapshot) {
            try {
                source.sample(writer, nowMs);
            } catch (RuntimeException e) {
                log.error("Metric source {} failed", source.getClass().getSimpleName(), e);
            }
        }
        synchronized (pendingEvents) {
            writer.append(pendingEvents);
            pendingEvents.setLength(0);
        }
        if (writer.isEmpty()) {
            return;
        }
        flush(writer.toString());
    }

    private void flush(String body) {
        try {
            sender.send(body);
            if (failing) {
                log.info("Metrics upload recovered");
                failing = false;
            }
            log.debug("Metrics flushed, {} bytes", body.length());
        } catch (IOException | RuntimeException e) {
            // first failure at WARN, the rest at DEBUG so an outage does not flood the log
            if (!failing) {
                log.warn("Metrics upload failed, dropping {} bytes: {}", body.length(), e.toString());
                failing = true;
            } else {
                log.debug("Metrics upload still failing, dropping {} bytes: {}", body.length(), e.toString());
            }
        }
    }
}
