package controller.actor;

import node.NodePin;

public interface ActorListener {
    NodePin getPin();
    void onAction(IOnOffActor actor, boolean invert);
    void notifyRegistered(IOnOffActor actor);
}
