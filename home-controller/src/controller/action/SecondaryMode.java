package controller.action;

import controller.actor.ActorListener;
import controller.actor.IReadableOnOff;
import org.apache.log4j.Logger;

import java.util.Date;

public class SecondaryMode {
    static Logger LOGGER = Logger.getLogger(LouversActionGroup.class.getName());
    // in ms
    private final int timeout;
    private final ActorListener indicator;
    private final IReadableOnOff indicatorSource;
    Thread timeoutThread;
    private boolean active;
    private long lastAccessTime = 0;

    public SecondaryMode(int timeout, ActorListener indicator) {
        this.timeout = timeout;
        indicatorSource = () -> isActive(now());
        this.indicator = indicator;
        this.indicator.addSource(indicatorSource);
    }

    /**
     * @return Activity state after switch
     */
    public boolean switchState() {
        // call isActive() to apply timeout if occurred
        isActiveAndTouch();
        if (active) {
            deactivate();
        } else {
            activate();
        }
        return active;
    }

    private synchronized void activate() {
        active = true;
        lastAccessTime = now();
        indicator.onAction(indicatorSource, false);
        // create guard thread for indicator
        timeoutThread = new Thread(() -> {
            long now;
            while ((now = now()) < lastAccessTime + timeout) {
                try {
                    Thread.sleep(lastAccessTime + timeout - now);
                } catch (InterruptedException e) {
                    return;
                }
            }
            deactivate();
        });
        timeoutThread.start();
        LOGGER.debug("SecondaryMode activated");
    }

    private synchronized void deactivate() {
        active = false;
        lastAccessTime = 0;
        LOGGER.debug("SecondaryMode deactivated");
        indicator.onAction(indicatorSource, false);
        if (timeoutThread != null) {
            timeoutThread.interrupt();
        }
    }

    public boolean isActiveAndTouch() {
        long now = now();
        LOGGER.debug("isActive(" + now + ")");
        if (isActive(now)) {
            lastAccessTime = now;
        } else {
            active = false;
        }
        LOGGER.debug("  isActive() returns " + active);
        return active;
    }

    private boolean isActive(long now) {
        return active && (lastAccessTime + timeout > now);
    }

    private long now() {
        return new Date().getTime();
    }
}
