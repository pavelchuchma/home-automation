package org.chuma.homecontroller.controller.controller;

public interface LouversController {
    String getId();

    String getLabel();

    Activity getActivity();

    boolean isUp();

    boolean isDown();

    /**
     * @return 0 - open, 1 - closed
     */
    double getPosition();

    /**
     * @return 0 - horizontal (open), 1 - closed
     */
    double getOffset();

    void up();

    void blind();

    /**
     * @param offset 0 - horizontal (open), 1 - closed
     */
    void outshine(double offset);

    void stop();
}
