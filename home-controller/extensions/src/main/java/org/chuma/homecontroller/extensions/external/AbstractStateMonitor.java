package org.chuma.homecontroller.extensions.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static java.lang.Math.min;


public abstract class AbstractStateMonitor<S> {
    static class State<S> {
        final long timestamp;
        final S value;

        public State(S value) {
            timestamp = System.currentTimeMillis();
            this.value = value;
        }
    }

    private static final Object refreshLoopLock = new Object();
    private static final Logger log = LoggerFactory.getLogger(AbstractStateMonitor.class.getName());
    protected final int refreshInternalMs;
    protected final int maxUnusedRunTimeMs;
    private final String name;
    protected boolean running;
    State<S> state;
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
            synchronized (refreshLoopLock) {
                try {
                    final long now = System.currentTimeMillis();
                    if (lastUseTime + maxUnusedRunTimeMs < now) {
                        log.trace("nobody needs me. Cleaning state and going to sleep");
                        state = null;
                        refreshLoopLock.wait();
                        log.trace("woke up");
                    } else {
                        long sleepTime = getSleepTime(now);
                        if (sleepTime <= 0) {
                            // time to try next refresh
                            break;
                        } else {
                            log.trace("going to sleep for {} ms before next refresh", sleepTime);
                            // wait a moment for next refresh time
                            refreshLoopLock.wait(sleepTime);
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

        state = new State<>(getStateImpl());
    }

    private long getSleepTime(long now) {
        if (state == null) {
            return -1;
        } else {
            return min(state.timestamp + refreshInternalMs, lastUseTime + maxUnusedRunTimeMs) - now;
        }
    }

    protected abstract S getStateImpl();

    public synchronized void stop() {
        log.trace("stopping {}", name);
        running = false;
        synchronized (refreshLoopLock) {
            refreshLoopLock.notify();
        }
        state = null;
    }

    public synchronized S getState() {
        log.trace("getting {} state", name);
        if (!running) {
            throw new IllegalStateException("Monitor is not running");
        }

        lastUseTime = System.currentTimeMillis();
        State<S> s = state;
        if (s == null) {
            synchronized (refreshLoopLock) {
                // wake up thread to start refresh
                refreshLoopLock.notify();
            }
            log.trace("getting state - not value ready, returning null");
            return null;
        }
        log.trace("getting state - done, returning state {} ms old", System.currentTimeMillis() - s.timestamp);
        return s.value;
    }

    /**
     * Returns stored State or synchronously gets new one if no valid state is stored
     *
     * @param forceRefresh force getting fresh state, regardless valid state is already stored.
     */
    public synchronized S getStateSync(boolean forceRefresh) {
        log.trace("getting {} state sync", name);
        lastUseTime = System.currentTimeMillis();
        State<S> s = state;
        if (s != null && !forceRefresh) {
            // fresh enough
            log.trace("getting state sync - done, returning state {} ms old", System.currentTimeMillis() - s.timestamp);
            return s.value;
        }
        S stateImpl = getStateImpl();
        if (running) {
            state = new State<>(stateImpl);
            synchronized (refreshLoopLock) {
                // wake up thread to schedule cleanup
                refreshLoopLock.notify();
            }
        }
        return stateImpl;
    }
}
