package org.chuma.homecontroller.extensions.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractStateMonitor<S> {
    private static final Object lock = new Object();
    private static final Logger log = LoggerFactory.getLogger(AbstractStateMonitor.class.getName());
    protected final int refreshInternalMs;
    protected final int maxUnusedRunTimeMs;
    private final String name;
    protected boolean running;
    S state;
    private long lastRefreshAttemptTime;
    private long lastUseTime;


    public AbstractStateMonitor(String name, int refreshInternalMs, int maxUnusedRunTimeMs) {
        this.name = name;
        running = false;
        this.refreshInternalMs = refreshInternalMs;
        this.maxUnusedRunTimeMs = maxUnusedRunTimeMs;
    }

    public synchronized void start() {
        running = true;
        new Thread(() -> {
            log.info("Starting {}", name);
            while (running) {
                refreshLoopBody();
            }
            log.info("{} stopped", name);
        }, name).start();
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

        lastRefreshAttemptTime = System.currentTimeMillis();
        state = getStateImpl();
    }

    protected abstract S getStateImpl();

    public synchronized void stop() {
        log.trace("stopping {}", name);
        running = false;
        synchronized (lock) {
            lock.notify();
        }
    }

    public synchronized S getState() {
        log.trace("getting {} state", name);
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

    public synchronized S getStateSync() {
        log.trace("getting {} state", name);
        S s = state;
        if (s != null) {
            // fresh enough
            return s;
        }
        return getStateImpl();
    }
}
