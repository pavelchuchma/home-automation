package org.chuma.homecontroller.controller.actor;

import org.chuma.homecontroller.nodes.node.NodePin;

public class TestingOnOffActor extends OnOffActor {
    public TestingOnOffActor(String id, NodePin output, int initValue, int onValue, ActorListener... actorListeners) {
        super(id, "LABEL", output, initValue, onValue, actorListeners);
    }

    @Override
    protected boolean setPinValue(NodePin nodePin, int value, int retryCount) {
        return (nodePin == null) ? true : super.setPinValue(nodePin, value, retryCount);
    }
}