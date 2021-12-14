package servlet;

import org.chuma.hvaccontroller.device.HvacDevice;

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
        appendNameValue(b, "defrost", hvacDevice.isDefrost());
        b.append(',');
        appendNameValue(b, "targetTemperature", hvacDevice.getTargetTemperature());
        b.append(',');
        appendNameValue(b, "airTemperature", hvacDevice.getAirTemperature());
        b.append(',');
        appendNameValue(b, "air2Temperature", hvacDevice.getAir2Temperature());
        b.append(',');
        appendNameValue(b, "roomTemperature", hvacDevice.getRoomTemperature());
        b.append(',');
        appendNameValue(b, "unitTemperature", hvacDevice.getUnitTemperature());
        b.append('}');

        return b.toString();
    }

}
