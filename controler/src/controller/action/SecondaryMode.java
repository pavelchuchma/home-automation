package controller.action;

import java.util.Date;

import controller.actor.AbstractActor;
import node.NodePin;
import org.apache.log4j.Logger;

public class SecondaryMode {
    static Logger LOGGER = Logger.getLogger(LouversActionGroup.class.getName());
    // in ms
    private final int timeout;
    NodePin indicatorPin;
    Thread timeoutThread;
    private boolean active;
    private long lastAccessTime = 0;

    public SecondaryMode(int timeout, NodePin indicatorPin) {
        this.timeout = timeout;
        this.indicatorPin = indicatorPin;
    }

    /**
     * @param value condition value
     * @return True if mode was changed
     */
    public boolean set(boolean value) {
        if (value) {
            // call isActive() to apply timeout if occurred
            isActive();
            if (active) {
                // second set of positive value turns state off
                deactivate();
            } else {
                activate();
            }
            return true;
        }
        return false;
    }

    private synchronized void activate() {
        active = true;
        lastAccessTime = now();
        setIndicatorValue(true);
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

    private void setIndicatorValue(boolean value) {
        if (indicatorPin != null) {
            int resultValue = (value) ? 0 : 1;
            AbstractActor.setPinValueImpl(indicatorPin, resultValue, 2);
        }
    }

    private synchronized void deactivate() {
        active = false;
        lastAccessTime = 0;
        LOGGER.debug("SecondaryMode deactivated");
        setIndicatorValue(false);
        if (timeoutThread != null) {
            timeoutThread.interrupt();
        }
    }

    public boolean isActive() {
        long now = now();
        LOGGER.debug("isActive(" + now + ")");
        if (active && (lastAccessTime + timeout > now)) {
            lastAccessTime = now;
        } else {
            active = false;
        }
        LOGGER.debug("  isActive() returns " + active);
        return active;
    }

    private long now() {
        return new Date().getTime();
    }
}
