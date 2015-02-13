package controller.actor;

public interface Actor {
    String getId();

    //abstract NodePin[] getOutputPins();

    boolean setValue(int val, Object actionData);

    Object getLastActionData();

    public void removeActionData();

    int getValue();
}