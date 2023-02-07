package org.chuma.homecontroller.controller.controller;

public class LouversPosition {
    public static final int DOWN_TOLERANCE = 10;
    Position position;
    Position offset;
    int upReserve;

    public LouversPosition(int maxPositionMs, int maxOffsetMs, int upReserve) {
        position = new Position(maxPositionMs);
        offset = new Position(maxOffsetMs);
        this.upReserve = upReserve;
    }

    /**
     * @return expected count of milliseconds to up position
     */
    public int startUp() {
        long now = stopImpl();
        offset.startUp(now);
        return position.startUp(now) + upReserve;
    }

    /**
     * @return expected count of milliseconds to down position
     */
    public int startDown() {
        long now = stopImpl();

        offset.startDown(now);
        return position.startDown(now);
    }

    private long stopImpl() {
        long now = now();
        position.stop(now);
        offset.stop(now);
        return now;
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    public void stop() {
        stopImpl();
    }

    public int getPosition() {
        return position.getPositionMs(now());
    }

    public int getOffset() {
        return offset.getPositionMs(now());
    }

    public boolean isDown() {
        return getPosition() >= position.maxPositionMs - offset.maxPositionMs - DOWN_TOLERANCE;
    }

    public void invalidate() {
        position.invalidate();
        offset.invalidate();
    }

    public Activity getActivity() {
        return position.activity;
    }
}