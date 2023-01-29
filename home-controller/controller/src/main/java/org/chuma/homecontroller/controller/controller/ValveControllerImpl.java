package org.chuma.homecontroller.controller.controller;

import org.chuma.homecontroller.base.node.OutputNodePin;
import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class ValveControllerImpl implements ValveController {
    private final LouversControllerImpl impl;

    public ValveControllerImpl(String id, String name, IOnOffActor upActor, IOnOffActor downActor, int downPositionMs) {
        impl = new LouversControllerImpl(id, name, upActor, downActor, downPositionMs, 0);
    }

    public ValveControllerImpl(String id, String name, OutputNodePin relayUp, OutputNodePin relayDown, int downPositionMs) {
        impl = new LouversControllerImpl(id, name, relayUp, relayDown, downPositionMs, 0);
    }

    @Override
    public String getId() {
        return impl.getId();
    }

    @Override
    public String getLabel() {
        return impl.getLabel();
    }

    @Override
    public Activity getActivity() {
        return impl.getActivity();
    }

    @Override
    public boolean isOpen() {
        return impl.isUp();
    }

    @Override
    public boolean isClosed() {
        return impl.isDown();
    }

    @Override
    public double getPosition() {
        return impl.getPosition();
    }

    @Override
    public void setPosition(double value) {
        impl.setPosition(value, 0);
    }

    @Override
    public void open() {
        impl.setPosition(0, 0);
    }

    @Override
    public void close() {
        impl.setPosition(1, 0);
    }
}
