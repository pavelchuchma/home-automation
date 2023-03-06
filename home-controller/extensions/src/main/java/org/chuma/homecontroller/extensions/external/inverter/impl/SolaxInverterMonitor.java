package org.chuma.homecontroller.extensions.external.inverter.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.extensions.external.inverter.InverterMonitor;
import org.chuma.homecontroller.extensions.external.inverter.InverterState;

/**
 * Monitoring service of Solax X3-Hybrid G4 Inverter
 * <p>
 * It runs periodic refresh in a background thread because inverter response time is ~1.2s.
 * <p>
 * Refresh thread is stopped if monitor is not used for a specified time and restarted after next request.
 */
public class SolaxInverterMonitor implements InverterMonitor {
    protected static Logger log = LoggerFactory.getLogger(SolaxInverterMonitor.class.getName());
    private static final Object lock = new Object();
    InverterState state;
    private boolean running;
    private long lastRefreshAttemptTime;
    private long lastUseTime;
    private final SolaxInverterLocalClient client;
    private final int refreshInternalMs;
    private final int maxUnusedRunTimeMs;

    public SolaxInverterMonitor(String url, String password, int refreshInternalMs, int maxUnusedRunTimeMs) {
        this.refreshInternalMs = refreshInternalMs;
        this.maxUnusedRunTimeMs = maxUnusedRunTimeMs;
        running = false;
        client = new SolaxInverterLocalClient(url, password);
    }

    @Override
    public synchronized void start() {
        running = true;
        new Thread(() -> {
            log.info("Starting SolaxMonitor");
            while (running) {
                refreshLoopBody();
            }
            log.info("SolaxMonitor stopped");
        }, "SolaxMonitor").start();
    }

    private void refreshLoopBody() {
        log.trace("entering refreshLoopBody");
        while (true) {
            synchronized (lock) {
                try {
                    final long now = System.currentTimeMillis();
                    if (lastUseTime + maxUnusedRunTimeMs < now) {
                        log.trace("nobody needs me. Cleaning state and going to sleep");
                        state = null;
                        lock.wait();
                        log.trace("woke up");
                    } else {
                        long sleepTime = lastRefreshAttemptTime + refreshInternalMs - now;
                        if (sleepTime <= 0) {
                            // time to try next refresh
                            break;
                        } else {
                            log.trace("going to sleep for {} ms before next refresh", sleepTime);
                            // wait a moment for next refresh time
                            lock.wait(sleepTime);
                            log.trace("woke up");
                        }
                    }
                } catch (InterruptedException e) {
                    //no action
                }
            }
            if (!running) {
                // stopped, no more fun
                return;
            }
        }
        try {
            lastRefreshAttemptTime = System.currentTimeMillis();
            log.debug("refreshing solax state");
            state = client.getState();
            log.trace("done in {} ms, inverter state {}", System.currentTimeMillis() - lastRefreshAttemptTime, state.getMode());
        } catch (Exception e) {
            log.error("Failed to refresh SolaxInverter state from " + client.getUrl(), e);
            state = null;
        }
    }

    @Override
    public synchronized void stop() {
        log.trace("stopping");
        running = false;
        synchronized (lock) {
            lock.notify();
        }
    }

    /**
     * Get last known state. Can be null in case of the first request or if it was not used longer than maxUnusedRunTimeMs time.
     */
    @Override
    public synchronized InverterState getState() {
        log.trace("getting state");
        if (!running) {
            throw new IllegalStateException("Monitor is not running");
        }
        lastUseTime = System.currentTimeMillis();
        synchronized (lock) {
            lock.notify();
        }
        log.trace("getting state - done");
        return state;
    }
}
