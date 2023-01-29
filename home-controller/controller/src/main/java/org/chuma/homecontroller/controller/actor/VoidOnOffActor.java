package org.chuma.homecontroller.controller.actor;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.OutputNodePin;
import org.chuma.homecontroller.base.node.Pin;
import org.chuma.homecontroller.base.packet.PacketUartIOMock;

/**
 * Implementation of OnOffActor without pin binding. Usable for just for registration of listeners.
 */
public class VoidOnOffActor extends OnOffActor {
    static Node fakeNode = new Node(-1, "fakeNode", new PacketUartIOMock());

    public VoidOnOffActor(String id, ActorListener... actorListeners) {
        this(id, true, actorListeners);
    }

    public VoidOnOffActor(String id, boolean highValueMeansOn, ActorListener... actorListeners) {
        super(id, "test_" + id, new OutputNodePin("fake" + id, "fakePin", fakeNode, Pin.pinA0, highValueMeansOn), actorListeners);
    }

    @Override
    protected boolean setPinValue(OutputNodePin nodePin, int value, int retryCount) {
        return true;
    }
}