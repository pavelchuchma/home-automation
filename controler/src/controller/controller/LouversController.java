package controller.controller;

public interface LouversController {
    String getName();

    Position.Activity getActivity();

    void up();

    void blind();

    void outshine(int percent);

    void stop();
}
