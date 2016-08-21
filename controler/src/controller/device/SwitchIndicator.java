package controller.device;

import java.util.ArrayList;

import controller.actor.AbstractActor;
import controller.actor.ActorListener;
import controller.actor.IOnOffActor;
import node.NodePin;

public class SwitchIndicator implements ActorListener {

    private static final int RETRY_COUNT = 2;
    Mode mode;

    NodePin pin;
    //todo: Build on Actor after removal onValue from Actor (1 must stand for ON in all cases)
    ArrayList<IOnOffActor> actors = new ArrayList<>();
    private int lastSetValue = -1;

    public SwitchIndicator(NodePin pin, Mode mode) {
        this.pin = pin;
        this.mode = mode;
    }

    @Override
    public void onAction(IOnOffActor actor, boolean invert) {
        if (!actors.contains(actor)) {
            throw new IllegalArgumentException("Cannot call onAction() with unregistered actor");
        }

        boolean val = false;
        switch (mode) {
            case SIGNAL_ALL_OFF:
                val = !isAnyOn();
                break;
            case SIGNAL_ANY_ON:
                val = isAnyOn();
                break;
        }

        int indVal = (val ^ invert) ? 1 : 0;
        if (indVal != lastSetValue) {
            if (AbstractActor.setPinValueImpl(pin, indVal, RETRY_COUNT)) {
                lastSetValue = indVal;
            }
        }
    }

    private boolean isAnyOn() {
        for (IOnOffActor a : actors) {
            if (a.isOn()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public NodePin getPin() {
        return pin;
    }

    @Override
    public void notifyRegistered(IOnOffActor actor) {
        if (actors.contains(actor)) {
            throw new IllegalArgumentException("Actor " + actor.toString() + " is already registered");
        }
        actors.add(actor);
    }

    public enum Mode {
        SIGNAL_ANY_ON, // false
        SIGNAL_ALL_OFF // true
    }
}