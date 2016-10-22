package controller.action;

import java.util.Date;

import org.apache.log4j.Logger;

public class SecondaryMode {
    static Logger LOGGER = Logger.getLogger(LouversActionGroup.class.getName());
    // in ms
    private final int timeout;
    private boolean active;
    private long lastAccessTime = 0;

    public SecondaryMode(int timeout) {
        this.timeout = timeout;
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
                active = false;
                lastAccessTime = 0;
                LOGGER.debug("Deactivating SecondaryMode");
            } else {
                active = true;
                lastAccessTime = now();
                LOGGER.debug("Activating SecondaryMode");
            }
            return true;
        }
        return false;
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
