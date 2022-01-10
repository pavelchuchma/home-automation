package org.chuma.homecontroller.extensions.actor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.chuma.homecontroller.controller.actor.IOnOffActor;
import org.chuma.homecontroller.extensions.external.MpdRadio;

public class RadioOnOffActor implements IOnOffActor {
    static Logger log = LoggerFactory.getLogger(RadioOnOffActor.class.getName());
    MpdRadio radio;

    public RadioOnOffActor(String serverAddress, String file) {
        this.radio = new MpdRadio(serverAddress, file);
    }

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
    public Object getActionData() {
        return null;
    }

    @Override
    public int getValue() {
        return (radio.isPlaying() ? 1 : 0);
    }

    @Override
    public void setActionData(Object actionData) {

    }

    @Override
    public void callListenersAndSetActionData(Object actionData) {

    }

    @Override
    public boolean isOn() {
        return radio.isPlaying();
    }
}
