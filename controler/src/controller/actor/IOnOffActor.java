package controller.actor;

public interface IOnOffActor extends Actor, IReadableOnOff {
    default boolean switchOn(Object actionData) {
        return switchOn(100, actionData);
    }

    boolean switchOn(int percent, Object actionData);

    boolean switchOff(Object actionData);
}
