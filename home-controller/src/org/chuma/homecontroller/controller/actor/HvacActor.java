package org.chuma.homecontroller.controller.actor;

import org.chuma.hvaccontroller.device.FanSpeed;
import org.chuma.hvaccontroller.device.HvacDevice;
import org.chuma.hvaccontroller.device.OperatingMode;

import java.util.Calendar;

public class HvacActor extends AbstractActor implements IOnOffActor {
    private HvacDevice hvacDevice;

    public HvacActor(HvacDevice hvacDevice, String id, String label, ActorListener... actorListeners) {
        super(id, label, actorListeners);
        this.hvacDevice = hvacDevice;
    }

    @Override
    public boolean switchOn(int percent, Object actionData) {
        return setValue(1, actionData);
    }

    @Override
    public boolean switchOff(Object actionData) {
        return setValue(0, actionData);
    }

    @Override
    public boolean setValue(int val, Object actionData) {
        if (hvacDevice == null) {
            return false;
        }
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
        if (currentMonth >= 6 && currentMonth <= 9) {
            hvacDevice.set(val == 1, OperatingMode.COOL, FanSpeed.SPEED_1, 25, false, false);
        } else {
            hvacDevice.set(val == 1, OperatingMode.HEAT, FanSpeed.SPEED_1, 23, false, false);
        }
        callListenersAndSetActionData(false, actionData);
        return true;
    }

    @Override
    public int getValue() {
        return (isOn()) ? 1 : 0;
    }

    @Override
    public boolean isOn() {
        return hvacDevice.isRunning();
    }

    public HvacDevice getHvacDevice() {
        return hvacDevice;
    }
}
