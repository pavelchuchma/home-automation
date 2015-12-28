package controller.controller;

public class LouversPosition {
    Position position;
    Position offset;
    int upReserve;

    public LouversPosition(int maxPositionMs, int maxOffsetMs, int upReserve) {
        position = new Position(maxPositionMs);
        offset = new Position(maxOffsetMs);
        this.upReserve = upReserve;
    }

    public int startUp() {
        long now = stopImpl();
        offset.startUp(now);
        return position.startUp(now) + upReserve;
    }

    public int startDown() {
        long now = stopImpl();

        offset.startDown(now);
        return position.startDown(now);
    }

    private long stopImpl() {
        long now = System.currentTimeMillis();
        position.stop(now);
        offset.stop(now);
        return now;
    }

    public void stop() {
        stopImpl();
    }

    public int getPosition() {
        return position.getPositionMs(System.currentTimeMillis());
    }

    public int getOffset() {
        return offset.getPositionMs(System.currentTimeMillis());
    }

    public boolean isDown() {
        return getPosition() >= position.maxPositionMs - offset.maxPositionMs;
    }

    public void invalidate() {
        position.invalidate();
        offset.invalidate();
    }

    public Position.Activity getActivity() {
        return position.activity;
    }
}