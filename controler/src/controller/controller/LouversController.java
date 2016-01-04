package controller.controller;

public interface LouversController {
    String getName();

    Activity getActivity();

    boolean isUp();

    boolean isDown();

    double getOffsetPercent();

    void up();

    void blind();

    void outshine(int percent);

    void stop();
}
