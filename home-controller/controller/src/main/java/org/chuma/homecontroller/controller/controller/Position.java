package org.chuma.homecontroller.controller.controller;

import org.chuma.homecontroller.controller.persistence.StateMap;

/**
 * Keeps an estimate of a slider position based on its time range and timing of calls of start/stop methods.
 *
 * It persists its state to StateMap on each stop() and cleans the state on start*() calls.
 */
class Position {
    final int maxPositionMs;
    private final String id;
    private final StateMap stateMap;
    // current position. Negative means unknown, 0 means up, maxPositionMs means down
    int positionMs;
    boolean positionIsKnown;
    Activity activity = Activity.stopped;
    long activityStartTime;

    Position(int maxPositionMs, String id, StateMap stateMap) {
        this.maxPositionMs = maxPositionMs;
        this.id = id;
        this.stateMap = stateMap;

        Integer lastState = stateMap.getValue(id);
        if (lastState != null) {
            positionMs = lastState;
            positionIsKnown = true;
        } else {
            positionMs = 0;
            positionIsKnown = false;
        }
    }

    /**
     * @return expected count of milliseconds to up position
     */
    int startUp(long currentTime) {
        recalculate(currentTime);
        stateMap.removeValue(id);
        activityStartTime = currentTime;
        activity = Activity.movingUp;
        return (positionIsKnown) ? positionMs : Math.min(maxPositionMs + positionMs, maxPositionMs);
    }

    /**
     * @return expected count of milliseconds to down position
     */
    int startDown(long currentTime) {
        recalculate(currentTime);
        stateMap.removeValue(id);
        activityStartTime = currentTime;
        activity = Activity.movingDown;
        return (positionIsKnown) ? maxPositionMs - positionMs : Math.min(maxPositionMs - positionMs, maxPositionMs);
    }

    void stop(long currentTime) {
        recalculate(currentTime);
        activity = Activity.stopped;
        if (positionIsKnown) {
            stateMap.setValue(id, positionMs);
        } else {
            stateMap.removeValue(id);
        }
    }

    private void recalculate(long currentTime) {
        if (activity == Activity.stopped) {
            // nothing to do
            return;
        }

        int duration = (int) (currentTime - activityStartTime);

        if (activity == Activity.movingDown) {
            positionMs = Math.min(positionMs + duration, maxPositionMs);
            if (!positionIsKnown && positionMs == maxPositionMs) {
                positionIsKnown = true;
            }
        } else { // moving up
            if (positionIsKnown) {
                positionMs = Math.max(positionMs - duration, 0);
            } else {
                positionMs -= duration;
                if (positionMs <= -maxPositionMs) {
                    positionMs = 0;
                    positionIsKnown = true;
                }
            }
        }
        activityStartTime = currentTime;
    }

    /**
     * @return Negative means unknown, 0 means up, maxPositionMs means down
     */
    public int getPositionMs(long currentTime) {
        recalculate(currentTime);
        return (positionIsKnown) ? positionMs : -1;
    }

    public void invalidate() {
        stateMap.removeValue(id);
        activity = Activity.stopped;
        activityStartTime = -1;
        positionIsKnown = false;
    }
}