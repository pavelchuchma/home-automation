package controller.actor;

public interface Actor {
    String getName();

    //abstract NodePin[] getOutputPins();

    boolean setValue(int val, Object actionData);

    Object getLastActionData();

    public void removeActionData();

    int getValue();

    void setActionData(Object actionData);

    void setIndicatorsAndActionData(boolean invert, Object actionData);
}