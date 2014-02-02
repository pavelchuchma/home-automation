package controller.actor;

import node.Pin;

public class OnOffActor extends AbstractActor {

    int maxValue;
    int onValue;
    int offValue;

    int value;

    public OnOffActor(String id, int nodeId, Pin pin, int initValue, int maxValue, int onValue, int offValue) {
        super(id, nodeId, pin, initValue);
        this.maxValue = maxValue;
        this.onValue = onValue;
        this.offValue = offValue;

        this.value = initValue;
    }

    @Override
    public int getPinOutputMask() {
        return 2 << pin.ordinal();
    }
}