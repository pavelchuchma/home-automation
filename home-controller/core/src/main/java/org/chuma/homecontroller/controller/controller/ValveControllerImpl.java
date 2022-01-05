package org.chuma.homecontroller.controller.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.chuma.homecontroller.controller.actor.IOnOffActor;
import org.chuma.homecontroller.nodes.node.NodePin;

public class ValveControllerImpl implements ValveController {
    static Logger log = LoggerFactory.getLogger(ValveControllerImpl.class.getName());
    private LouversControllerImpl impl;

    public ValveControllerImpl(String id, String name, IOnOffActor upActor, IOnOffActor downActor, int downPositionMs) {
        impl = new LouversControllerImpl(id, name, upActor, downActor, downPositionMs, 0);
    }

    public ValveControllerImpl(String id, String name, NodePin relayUp, NodePin relayDown, int downPositionMs) {
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
    public void setPosition(int percent) {
        impl.setPosition(percent, 0);
    }

    @Override
    public void open() {
        impl.setPosition(0, 0);
    }

    @Override
    public void close() {
        impl.setPosition(100, 0);
    }
}
