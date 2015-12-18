package controller.controller;

public class LouversPosition {
    Position position;
    Position offset;

    public LouversPosition(int maxPositionMs, int maxOffsetMs) {
        position = new Position(maxPositionMs);
        offset = new Position(maxOffsetMs);
    }

    int up() {
        long now = stopImpl();

        offset.up(now);
        return position.up(now);
    }

    int down() {
        long now = stopImpl();

        offset.down(now);
        return position.down(now);
    }

    private long stopImpl() {
        long now = System.currentTimeMillis();
        position.stop(now);
        offset.stop(now);
        return now;
    }

    void stop() {
        stopImpl();
    }

    int getPosition() {
        return position.getPositionMs(System.currentTimeMillis());
    }

    int getOffset() {
        return offset.getPositionMs(System.currentTimeMillis());
    }


}