package org.chuma.homecontroller.extensions.actor;

import java.util.Calendar;

import org.chuma.homecontroller.controller.actor.AbstractActor;
import org.chuma.homecontroller.controller.actor.ActorListener;
import org.chuma.homecontroller.controller.actor.IOnOffActor;
import org.chuma.hvaccontroller.device.FanSpeed;
import org.chuma.hvaccontroller.device.HvacDevice;
import org.chuma.hvaccontroller.device.OperatingMode;

public class HvacActor extends AbstractActor implements IOnOffActor {
    private final HvacDevice hvacDevice;

    public HvacActor(HvacDevice hvacDevice, String id, String label, ActorListener... actorListeners) {
        super(id, label, actorListeners);
        this.hvacDevice = hvacDevice;
    }

    @Override
    public boolean switchOn(Object actionData) {
        return setValue(true, actionData);
    }

    @Override
    public boolean switchOff(Object actionData) {
        return setValue(false, actionData);
    }

    private boolean setValue(boolean switchOn, Object actionData) {
        if (hvacDevice == null) {
            return false;
        }
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
        if (currentMonth >= 6 && currentMonth <= 9) {
            hvacDevice.set(switchOn, OperatingMode.COOL, FanSpeed.SPEED_1, 25, false, false);
        } else {
            hvacDevice.set(switchOn, OperatingMode.HEAT, FanSpeed.SPEED_1, 23, false, false);
        }
        callListenersAndSetActionData(actionData);
        return true;
    }

    @Override
    public boolean isOn() {
        return hvacDevice.isRunning();
    }

    public HvacDevice getHvacDevice() {
        return hvacDevice;
    }
}
