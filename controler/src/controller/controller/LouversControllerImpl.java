package controller.controller;

import controller.actor.IOnOffActor;

public class LouversControllerImpl implements LouversController {
    public LouversControllerImpl(int downPositionMs) {
//        this.maxPositionMs = maxPositionMs;
    }

    IOnOffActor upActor;
    IOnOffActor downActor;


    @Override
    public synchronized void up() {
        if (!downActor.switchOff(this)) {
            //bla
        }

    }

    @Override
    public synchronized void blind() {

    }

    @Override
    public synchronized void outshine() {

    }

}