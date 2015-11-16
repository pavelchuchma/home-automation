package controller.controller;

import controller.actor.IOnOffActor;

public class LouverControllerImpl implements LouverController {
    public LouverControllerImpl(int downPositionMs) {
//        this.downPositionMs = downPositionMs;
    }

    IOnOffActor upActor;
    IOnOffActor downActor;


    @Override
    public synchronized void open() {
        if (!downActor.switchOff(this)) {
            //bla
        }

    }

    @Override
    public void blind() {

    }

    @Override
    public void outshine() {

    }

}