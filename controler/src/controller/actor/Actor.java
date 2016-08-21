package controller.actor;

public interface Actor {
    String getId();

    String getLabel();

    boolean setValue(int val, Object actionData);

    Object getLastActionData();

    void removeActionData();

    int getValue();

    void setActionData(Object actionData);

    void callListenersAndSetActionData(boolean invert, Object actionData);
}