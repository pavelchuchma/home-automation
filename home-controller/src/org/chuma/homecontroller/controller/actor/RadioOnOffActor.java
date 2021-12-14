package org.chuma.homecontroller.controller.actor;

import org.apache.log4j.Logger;
import org.chuma.homecontroller.controller.external.MpdRadio;

public class RadioOnOffActor implements IOnOffActor {
    static Logger log = Logger.getLogger(RadioOnOffActor.class.getName());
    MpdRadio radio = new MpdRadio();

    @Override
    public boolean switchOn(int percent, Object actionData) {
        radio.start();
        return true;
    }

    @Override
    public boolean switchOff(Object actionData) {
        radio.stop();
        return true;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getLabel() {
        return "Radio";
    }

    @Override
    public boolean setValue(int val, Object actionData) {
        return false;
    }

    @Override
    public Object getLastActionData() {
        return null;
    }

    @Override
    public void removeActionData() {

    }

    @Override
    public int getValue() {
        return (radio.isPlaying() ? 1 : 0);
    }

    @Override
    public void setActionData(Object actionData) {

    }

    @Override
    public void callListenersAndSetActionData(boolean invert, Object actionData) {

    }

    @Override
    public boolean isOn() {
        return radio.isPlaying();
    }
}
