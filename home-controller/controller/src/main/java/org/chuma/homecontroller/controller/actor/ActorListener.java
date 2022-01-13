package org.chuma.homecontroller.controller.actor;

public interface ActorListener {
    void onAction(IReadableOnOff source, Object actionData);

    void addSource(IReadableOnOff source);
}
