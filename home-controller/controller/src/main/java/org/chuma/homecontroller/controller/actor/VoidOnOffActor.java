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
        super(id, "void" + id, new OutputNodePin("void" + id, "fakePin", fakeNode, Pin.pinA0, true), actorListeners);
    }

    @Override
    protected boolean setPinValue(OutputNodePin nodePin, boolean value, int retryCount) {
        return true;
    }
}