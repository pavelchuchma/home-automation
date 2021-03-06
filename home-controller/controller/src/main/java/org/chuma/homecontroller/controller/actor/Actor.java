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
     * @param val        target value. Acceptable values are from interval <0;1>.
     * @param actionData Optional action specific data. Each {@link Actor} instance holds one actionData instance
     *                   usable by following actions
     * @return true on success
     */
    boolean setValue(double val, Object actionData);

    /**
     * @return values are from interval <0;1>
     */
    double getValue();

    /**
     * @return The last stored action data.
     */
    Object getActionData();

    /**
     * Allows storing action's data to the actor instance.
     * Usually keeps a signature of an action who set the last actor value.
     */
    void setActionData(Object actionData);

    /**
     * Sets action data and calls all action listeners with actionData in a synchronized block.
     */
    void callListenersAndSetActionData(Object actionData);
}