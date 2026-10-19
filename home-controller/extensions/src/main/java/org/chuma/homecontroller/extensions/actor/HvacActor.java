package org.chuma.homecontroller.extensions.actor;

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
        return set(true, OperatingMode.NONE, FanSpeed.NONE, 0, null, null, actionData);
    }

    @Override
    public boolean switchOff(Object actionData) {
        return set(false, OperatingMode.NONE, FanSpeed.NONE, 0, null, null, actionData);
    }

    /**
     * Sends the given values to the unit; null, NONE and 0 keep the current value.
     *
     * @return false when there is no device or it has not reported its state yet
     */
    public boolean set(Boolean on, OperatingMode mode, FanSpeed fanSpeed, int temp, Boolean sleep, Boolean quiet, Object actionData) {
        // the set packet always carries mode and fan speed, unknown ones cannot be serialized
        if (hvacDevice == null || hvacDevice.getCurrentMode() == OperatingMode.NONE || hvacDevice.getFanSpeed() == FanSpeed.NONE) {
            return false;
        }
        hvacDevice.set(on, mode, fanSpeed, temp, sleep, quiet);
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
