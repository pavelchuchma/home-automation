package controller.controller;

class Position {
    enum Activity {
        movingUp,
        movingDown,
        stopped
    }

    int downPositionMs;
    // current position. Negative means unknown, 0 means up, downPositionMs means down
    int positionMs = 0;
    boolean positionIsKnown = false;
    Activity activity = Activity.stopped;
    long activityStartTime;

    Position(int downPositionMs) {
        this.downPositionMs = downPositionMs;
    }

    synchronized int up() {
        stop();
        activityStartTime = System.currentTimeMillis();
        activity = Activity.movingUp;
        return (positionIsKnown) ? positionMs : Math.min(downPositionMs + positionMs, downPositionMs);
    }

    synchronized int down() {
        stop();
        activityStartTime = System.currentTimeMillis();
        activity = Activity.movingDown;
        return (positionIsKnown) ? downPositionMs - positionMs : Math.min(downPositionMs - positionMs, downPositionMs);
    }

    synchronized void stop() {
        if (activity != Activity.stopped) {
            int duration = (int) (System.currentTimeMillis() - activityStartTime);

            if (activity == Activity.movingDown) {
                positionMs = Math.min(positionMs + duration, downPositionMs);
                if (!positionIsKnown && positionMs == downPositionMs) {
                    positionIsKnown = true;
                }
            } else { // moving up
                if (positionIsKnown) {
                    positionMs = Math.min(positionMs - duration, 0);
                } else {
                    positionMs -= duration;
                    if (positionMs <= -downPositionMs) {
                        positionMs = 0;
                        positionIsKnown = true;
                    }
                }
            }
            activity = Activity.stopped;
        }
    }

    synchronized public int getPositionMs() {
        return (positionIsKnown) ? positionMs : -1;
    }
}