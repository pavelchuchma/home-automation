package org.chuma.homecontroller.extensions.external.inverter.impl;

import com.github.cliftonlabs.json_simple.JsonArray;

public class RemoteConfiguration {
    public static int SYSTEM_ON = 47;
    public static int SELF_USE_MIN_SOC = 29;
    public static int SELF_USE_CHARGE_FROM_GRID = 30;
    public static int SELF_USE_CHARGE_BATTERY_TO = 31;
    public static int FEED_IN_PRIORITY_MIN_SOC = 32;
    public static int FEED_IN_PRIORITY_BATTERY_TO = 33;
    public static int BACKUP_MODE_MIN_SOC = 34;
    public static int BACKUP_MODE_CHARGE_BATTERY_TO = 35;
    private final int[] data;

    public RemoteConfiguration(JsonArray jsonArray) {
        // json array is indexed from 1
        data = new int[jsonArray.size() + 1];
        for (int i = 0; i < jsonArray.size(); i++) {
            data[i + 1] = jsonArray.getInteger(i);
        }
    }

    public int[] getData() {
        return data;
    }

    public int getSelfUseMinimalSoc() {
        return data[SELF_USE_MIN_SOC];
    }
}
