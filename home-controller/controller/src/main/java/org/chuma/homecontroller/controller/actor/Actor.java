package org.chuma.homecontroller.controller.actor;

/**
 * Represents a stateful device.
 */
public interface Actor {
    /**
     * @return unique ID
     */
    String getId();

    /**
     * @return Label (name) for UI
     */
    String getLabel();

    /**
     * @param val        target value
     * @param actionData Optional action specific data. Each {@link Actor} instance holds one actionData instance
     *                   usable by following actons
     * @return true on success
     */
    boolean setValue(int val, Object actionData);

    int getValue();

    /**
     * Allows storing action's data to the actor instance.
     * Usually keeps a signature of an action who set the last actor value.
     */
    void setActionData(Object actionData);

    /**
     * @return The last stored action data.
     */
    Object getActionData();

    /**
     * Sets action data and calls all action listeners with actionData in a synchronized block.
     */
    void callListenersAndSetActionData(Object actionData);
}