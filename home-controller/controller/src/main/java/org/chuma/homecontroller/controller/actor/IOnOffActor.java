package org.chuma.homecontroller.controller.actor;

public interface IOnOffActor extends Actor, IReadableOnOff {
    default boolean switchOn(Object actionData) {
        return switchOn(1d, actionData);
    }

    /**
     * @param value Target value from interval <0;1>
     * @param actionData modifier custom data
     * @return true on success
     */
    boolean switchOn(double value, Object actionData);

    boolean switchOff(Object actionData);
}
