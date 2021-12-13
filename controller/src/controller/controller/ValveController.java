package controller.controller;

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

    void setPosition(int percent);

    void open();

    void close();
}
