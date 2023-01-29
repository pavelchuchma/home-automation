package org.chuma.homecontroller.controller.actor;

import static org.chuma.homecontroller.base.packet.Packet.MAX_PWM_VALUE;

import org.chuma.homecontroller.controller.device.LddBoardDevice;

public class LddActor extends PwmActor {
    public LddActor(String name, String label, LddBoardDevice.LddNodePin lddNodePin, double maxLightCurrent, ActorListener... actorListeners) {
        super(name, label, lddNodePin, actorListeners);
        lddNodePin.setMaxOutputCurrent(maxLightCurrent);
    }

    public double getOutputCurrent() {
        return (double)currentPwmValue / MAX_PWM_VALUE * getLddOutput().getMaxLddCurrent();
    }

    public double getMaxOutputCurrent() {
        return (double)getLddOutput().getMaxPwmValue() / MAX_PWM_VALUE * getLddOutput().getMaxLddCurrent();
    }

    public LddBoardDevice.LddNodePin getLddOutput() {
        return (LddBoardDevice.LddNodePin)getPwmOutputNodePin();
    }
}
