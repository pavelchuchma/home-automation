package controller.actor;

import chuma.hvaccontroller.device.FanSpeed;
import chuma.hvaccontroller.device.HvacDevice;
import chuma.hvaccontroller.device.OperatingMode;

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
        hvacDevice.set(val == 1, OperatingMode.HEAT, FanSpeed.SPEED_1, 23, false, false);
        callListenersAndSetActionData(false, actionData);
        return true;
    }

    @Override
    public int getValue() {
        return (isOn()) ? 1 : 0;
    }

    @Override
    public boolean isOn() {
        return hvacDevice.isRunning() && hvacDevice.getTargetMode() == OperatingMode.HEAT;
    }

    public HvacDevice getHvacDevice() {
        return hvacDevice;
    }
}
