package controller.actor;

import node.NodePin;

public class TestingOnOffActor extends OnOffActor {
    public TestingOnOffActor(String id, NodePin output, int initValue, int onValue, Indicator... indicators) {
        super(id, output, initValue, onValue, indicators);
    }

    @Override
    protected boolean setPinValue(NodePin nodePin, int value, int retryCount) {
        return (nodePin == null) ? true : super.setPinValue(nodePin, value, retryCount);
    }
}