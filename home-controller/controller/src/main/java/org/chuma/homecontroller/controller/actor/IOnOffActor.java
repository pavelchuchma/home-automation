package org.chuma.homecontroller.controller.actor;

public interface IOnOffActor extends Actor, IReadableOnOff {
    /**
     * @param actionData modifier custom data
     * @return true on success
     */
    boolean switchOn(Object actionData);

    default boolean switchOn() {
        return switchOn(null);
    }

    boolean switchOff(Object actionData);

    default boolean switchOff() {
        return switchOff(null);
    }
}
