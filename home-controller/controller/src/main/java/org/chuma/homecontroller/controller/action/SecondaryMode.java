package org.chuma.homecontroller.controller.action;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.controller.actor.ActorListener;
import org.chuma.homecontroller.controller.actor.IReadableOnOff;

public class SecondaryMode {
    static Logger log = LoggerFactory.getLogger(LouversActionGroup.class.getName());
    // in ms
    private final int timeout;
    private final ActorListener indicator;
    private final IReadableOnOff indicatorSource;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> deactivateFuture;
    private long lastAccessTime = 0;

    public SecondaryMode(int timeoutMs, ActorListener indicator) {
        this.timeout = timeoutMs;
        indicatorSource = this::isActive;
        this.indicator = indicator;
        this.indicator.addSource(indicatorSource);
    }

    /**
     * @return Activity state after switch
     */
    public synchronized boolean switchState() {
        // call isActive() to apply timeout if occurred
        if (isActiveAndTouch()) {
            deactivate();
        } else {
            activate();
        }
        return isActive();
    }

    private synchronized void activate() {
        lastAccessTime = nowMs();
        endOrProlongActivation();
        indicator.onAction(indicatorSource, null);
        log.debug("SecondaryMode activated");
    }

    private synchronized void endOrProlongActivation() {
        long remainingMs = lastAccessTime + timeout - nowMs();
        if (remainingMs > 0) {
            deactivateFuture = scheduler.schedule(this::endOrProlongActivation,
                    remainingMs, TimeUnit.MILLISECONDS);
        } else {
            deactivate();
        }
    }

    private synchronized void deactivate() {
        log.debug("SecondaryMode deactivated");
        if (deactivateFuture != null) {
            deactivateFuture.cancel(false);
            deactivateFuture = null;
        }
        indicator.onAction(indicatorSource, null);
    }

    public synchronized boolean isActiveAndTouch() {
        if (isActive()) {
            lastAccessTime = nowMs();
        }
        log.debug("  isActive() returns {}", isActive());
        return isActive();
    }

    private boolean isActive() {
        return deactivateFuture != null;
    }

    private long nowMs() {
        return new Date().getTime();
    }
}
