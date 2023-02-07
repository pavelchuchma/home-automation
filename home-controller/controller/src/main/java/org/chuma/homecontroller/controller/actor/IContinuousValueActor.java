package org.chuma.homecontroller.controller.actor;

public interface IContinuousValueActor extends IOnOffActor {
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
     * @param value Target value from interval <0;1>
     * @param actionData modifier custom data
     * @return true on success
     */
    boolean switchOn(double value, Object actionData);
}
