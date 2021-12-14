package controller.controller;

class Position {

    int maxPositionMs;
    // current position. Negative means unknown, 0 means up, maxPositionMs means down
    int positionMs = 0;
    boolean positionIsKnown = false;
    Activity activity = Activity.stopped;
    long activityStartTime;

    Position(int maxPositionMs) {
        this.maxPositionMs = maxPositionMs;
    }

    /**
     * @param currentTime
     * @return expected count of milliseconds to up position
     */
    int startUp(long currentTime) {
        stop(currentTime);
        activityStartTime = currentTime;
        activity = Activity.movingUp;
        return (positionIsKnown) ? positionMs : Math.min(maxPositionMs + positionMs, maxPositionMs);
    }

    int startDown(long currentTime) {
        stop(currentTime);
        activityStartTime = currentTime;
        activity = Activity.movingDown;
        return (positionIsKnown) ? maxPositionMs - positionMs : Math.min(maxPositionMs - positionMs, maxPositionMs);
    }

    void stop(long currentTime) {
        recalculate(currentTime);
        activity = Activity.stopped;
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
     * @param currentTime
     * @return Negative means unknown, 0 means up, maxPositionMs means down
     */
    public int getPositionMs(long currentTime) {
        recalculate(currentTime);
        return (positionIsKnown) ? positionMs : -1;
    }

    public void invalidate() {
        activity = Activity.stopped;
        activityStartTime = -1;
        positionIsKnown = false;
    }
}