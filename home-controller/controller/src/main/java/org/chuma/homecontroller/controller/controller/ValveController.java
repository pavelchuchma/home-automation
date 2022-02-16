package org.chuma.homecontroller.controller.controller;

public interface ValveController {
    String getId();

    String getLabel();

    Activity getActivity();

    boolean isOpen();

    boolean isClosed();

    /**
     * @return 0 - open, 1 - closed
     */
    double getPosition();

    /**
     *
     * @param value 0 - open, 1 - closed
     */
    void setPosition(double value);

    void open();

    void close();
}
