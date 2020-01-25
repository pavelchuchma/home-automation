package servlet;

import chuma.hvaccontroller.device.HvacDevice;

public class HvacJsonSerializer extends AbstractJsonSerializer {
    public static String serialize(HvacDevice hvacDevice) {
        StringBuffer b = new StringBuffer("{");
        appendNameValue(b, "id", "hvac");
        b.append(',');
        appendNameValue(b, "on", hvacDevice.isRunning());
        b.append(',');
        appendNameValue(b, "fanSpeed", hvacDevice.getFanSpeed().toString());
        b.append(',');
        appendNameValue(b, "currentMode", hvacDevice.getCurrentMode().toString());
        b.append(',');
        appendNameValue(b, "targetMode", hvacDevice.getTargetMode().toString());
        b.append(',');
        appendNameValue(b, "autoMode", hvacDevice.isAutoMode());
        b.append(',');
        appendNameValue(b, "quiteMode", hvacDevice.isQuiteMode());
        b.append(',');
        appendNameValue(b, "sleepMode", hvacDevice.isSleepMode());
        b.append(',');
        appendNameValue(b, "targetTemperature", hvacDevice.getTargetTemperature());
        b.append(',');
        appendNameValue(b, "airTemperature", hvacDevice.getAirTemperature());
        b.append(',');
        appendNameValue(b, "x", hvacDevice.isX());
        b.append(',');
        appendNameValue(b, "y", hvacDevice.isY());
        b.append('}');

        return b.toString();
    }

}
