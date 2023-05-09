package org.chuma.homecontroller.controller.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.utils.Utils;
import org.chuma.homecontroller.controller.actor.Actor;
import org.chuma.homecontroller.controller.actor.OnOffActor;
import org.chuma.homecontroller.controller.device.Relay16BoardDevice;

public class Relay16TestLoopAction implements Action {
    static Logger log = LoggerFactory.getLogger(Relay16TestLoopAction.class.getName());
    private final Relay16BoardDevice relay16;

    public Relay16TestLoopAction(Relay16BoardDevice relay16) {
        this.relay16 = relay16;
    }

    @Override
    public void perform(int timeSinceLastAction) {
        for (int i = 1; i <= 16; i++) {
            OnOffActor actor = new OnOffActor("", ", ", relay16.getRelay(i));
            actor.switchOn();
            Utils.sleep(1000);
            actor.switchOff();
            Utils.sleep(500);
        }
    }

    @Override
    public Actor getActor() {
        return null;
    }
}
