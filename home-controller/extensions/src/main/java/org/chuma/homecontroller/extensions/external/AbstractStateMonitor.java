package org.chuma.homecontroller.extensions.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Periodically refreshes a device state in a background thread.
 * <p>
 * The thread runs nonstop at two rates: {@code refreshIntervalMs} for {@code fastRateDurationMs} after the last
 * {@link #getState()} / {@link #getStateSync(boolean)} call (somebody is watching the device in the UI), then
 * {@code idleRefreshIntervalMs}. {@link #peekState()} reads the last state without switching to the fast rate.
 */
public abstract class AbstractStateMonitor<S> {
    public static class Snapshot<S> {
        public final long timestamp;
        public final S value;

        public Snapshot(S value) {
            timestamp = System.currentTimeMillis();
            this.value = value;
        }
    }

    private final Object refreshLoopLock = new Object();
    private static final Logger log = LoggerFactory.getLogger(AbstractStateMonitor.class.getName());
    protected final int refreshIntervalMs;
    protected final int fastRateDurationMs;
    protected final int idleRefreshIntervalMs;
    private final String name;
    protected volatile boolean running;
    volatile Snapshot<S> state;
    private long lastUseTime;

    /**
     * @param refreshIntervalMs     refresh period while the state is being watched (fast rate)
     * @param fastRateDurationMs    how long the fast rate stays on after the last getState() call
     * @param idleRefreshIntervalMs refresh period when nobody is watching
     */
    public AbstractStateMonitor(String name, int refreshIntervalMs, int fastRateDurationMs, int idleRefreshIntervalMs) {
        this.name = name;
        running = false;
        this.refreshIntervalMs = refreshIntervalMs;
        this.fastRateDurationMs = fastRateDurationMs;
        this.idleRefreshIntervalMs = idleRefreshIntervalMs;
    }

    public boolean isRunning() {
        return running;
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
                    long sleepTime = getSleepTime(System.currentTimeMillis());
                    if (sleepTime <= 0) {
                        // time to try next refresh
                        break;
                    }
                    log.trace("going to sleep for {} ms before next refresh", sleepTime);
                    refreshLoopLock.wait(sleepTime);
                    log.trace("woke up");
                } catch (InterruptedException e) {
                    //no action
                }
            }
            if (!running) {
                // stopped, no more fun
                return;
            }
        }

        Snapshot<S> s = state;
        state = new Snapshot<>(getStateImpl(s == null));
    }

    private long getSleepTime(long now) {
        Snapshot<S> s = state;
        if (s == null) {
            return -1;
        }
        int interval = isFastRate(now) ? refreshIntervalMs : idleRefreshIntervalMs;
        return s.timestamp + interval - now;
    }

    private boolean isFastRate(long now) {
        return lastUseTime + fastRateDurationMs > now;
    }

    /**
     * @param firstRefresh true for the first refresh after {@link #start()}
     */
    protected abstract S getStateImpl(boolean firstRefresh);

    public synchronized void stop() {
        log.trace("stopping {}", name);
        running = false;
        synchronized (refreshLoopLock) {
            refreshLoopLock.notify();
        }
        state = null;
    }

    /**
     * Returns the last known state and switches the monitor to the fast rate.
     * Null before the first refresh or when the last refresh failed.
     */
    public synchronized S getState() {
        log.trace("getting {} state", name);
        if (!running) {
            throw new IllegalStateException("Monitor is not running");
        }

        boolean wasFast = isFastRate(System.currentTimeMillis());
        lastUseTime = System.currentTimeMillis();
        Snapshot<S> s = state;
        if (s == null) {
            log.trace("getting state - not value ready, returning null");
            return null;
        }
        if (!wasFast) {
            synchronized (refreshLoopLock) {
                // switching to fast rate, let the thread recompute its sleep time
                refreshLoopLock.notify();
            }
        }
        log.trace("getting state - done, returning state {} ms old", System.currentTimeMillis() - s.timestamp);
        return s.value;
    }

    /**
     * Returns the last known state without affecting the refresh rate. Intended for background consumers
     * (metrics reporting). Null when not running, before the first refresh, or when the last refresh failed.
     */
    public Snapshot<S> peekState() {
        if (!running) {
            return null;
        }
        Snapshot<S> s = state;
        return (s == null || s.value == null) ? null : s;
    }

    /**
     * Returns stored State or synchronously gets new one if no valid state is stored
     *
     * @param forceRefresh force getting fresh state, regardless valid state is already stored.
     */
    public synchronized S getStateSync(boolean forceRefresh) {
        log.trace("getting {} state sync", name);
        lastUseTime = System.currentTimeMillis();
        Snapshot<S> s = state;
        if (s != null && !forceRefresh) {
            // fresh enough
            log.trace("getting state sync - done, returning state {} ms old", System.currentTimeMillis() - s.timestamp);
            return s.value;
        }
        S stateImpl = getStateImpl(s == null);
        if (running) {
            state = new Snapshot<>(stateImpl);
            synchronized (refreshLoopLock) {
                // wake up thread to reschedule the next refresh
                refreshLoopLock.notify();
            }
        }
        return stateImpl;
    }
}
