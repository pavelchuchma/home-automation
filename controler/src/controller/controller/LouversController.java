package controller.controller;

public interface LouversController {
    String getId();

    String getLabel();

    Activity getActivity();

    boolean isUp();

    boolean isDown();

    /**
     * 0 - up, 1 - down
     * @return
     */
    double getPosition();

    /**
     * 0 - horizontal (open), 1 - closed
     * @return
     */
    double getOffset();

    void up();

    void blind();

    void outshine(int percent);

    void stop();
}
