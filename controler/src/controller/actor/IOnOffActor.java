package controller.actor;

public interface IOnOffActor extends Actor{
    boolean switchOn(Object actionData);

    boolean switchOff(Object actionData);

    boolean isOn();

    void setIndicatorsAndActionData(boolean invert, Object actionData);
}
