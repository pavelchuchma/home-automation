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