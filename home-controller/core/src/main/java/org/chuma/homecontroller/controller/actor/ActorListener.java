package org.chuma.homecontroller.controller.actor;

public interface ActorListener {
    void onAction(IReadableOnOff source, boolean invert);

    void addSource(IReadableOnOff source);
}
