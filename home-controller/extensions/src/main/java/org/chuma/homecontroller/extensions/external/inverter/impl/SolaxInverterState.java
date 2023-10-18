package org.chuma.homecontroller.extensions.external.inverter.impl;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;

import org.chuma.homecontroller.extensions.external.inverter.InverterState;

/**
 * Represents state of Solax Inverter
 * <p>
 * Implementation is based on <a href="https://github.com/squishykid/solax/blob/master/solax/inverters/x3_hybrid_g4.py">squishykid/solax project</a>
 */
public class SolaxInverterState implements InverterState {
    private static final int INT16_MAX = 0x7FFF;
    private static final int INT32_MAX = 0x7FFFFFFF;

    public final long timestamp;
    public final String version;
    public final String inverterSerialNumber;
    private final String wifiSerialNumber;

    public final Mode mode;
    public final BatteryMode batteryMode;
    public final double grid1Voltage;
    public final double grid2Voltage;
    public final double grid3Voltage;
    public final int grid1Power;
    public final int grid2Power;
    public final int grid3Power;
    public final int pv1Power;
    public final int pv2Power;
    public final int eps1Power;
    public final int eps2Power;
    public final int eps3Power;
    public final int feedInPower;
    public final int batteryPower;
    public final double yieldTotal;
    public final double yieldToday;
    public final double feedInEnergyTotal;
    public final double feedInEnergyToday;
    public final double consumedEnergyTotal;
    public final double consumedEnergyToday;
    public final int batterySoc;
    public final int batteryTemp;
    public final double batteryVoltage;

    public SolaxInverterState(JsonObject json) {
        timestamp = System.currentTimeMillis();
        version = json.get("ver").toString();
        inverterSerialNumber = ((JsonArray)json.get("Information")).get(2).toString();
        wifiSerialNumber = json.get("sn").toString();
        JsonArray data = (JsonArray)json.get("Data");
        mode = Mode.values()[data.getInteger(19)];
        batteryMode = BatteryMode.values()[data.getInteger(168)];

        grid1Voltage = toSigned16(data.getInteger(0)) / 10d;
        grid2Voltage = toSigned16(data.getInteger(1)) / 10d;
        grid3Voltage = toSigned16(data.getInteger(2)) / 10d;
        grid1Power = toSigned16(data.getInteger(6));
        grid2Power = toSigned16(data.getInteger(7));
        grid3Power = toSigned16(data.getInteger(8));
        pv1Power = data.getInteger(14);
        pv2Power = data.getInteger(15);
        eps1Power = toSigned16(data.getInteger(29));
        eps2Power = toSigned16(data.getInteger(30));
        eps3Power = toSigned16(data.getInteger(31));
        feedInPower = toSigned32(packU16(data, 34, 35));
        batteryPower = toSigned16(data.getLong(41));
        yieldTotal = packU16(data, 68, 69) / 10d;
        yieldToday = data.getInteger(70) / 10d;
        feedInEnergyTotal = packU16(data, 86, 87) / 100d;
        feedInEnergyToday = data.getInteger(90) / 100d;
        consumedEnergyTotal = packU16(data, 88, 89) / 100d;
        consumedEnergyToday = data.getInteger(92) / 100d;
        batterySoc = data.getInteger(103);
        batteryTemp = toSigned16(data.getInteger(105));
        batteryVoltage = packU16(data, 169, 170) / 100d;
    }

    private static long packU16(JsonArray data, int i1, int i2) {
        return data.getInteger(i1) + (data.getLong(i2) << 16);
    }

    private static int toSigned32(long val) {
        return (int)((val > INT32_MAX) ? val - (2L << 31) : val);
    }

    private static int toSigned16(long val) {
        return (int)((val > INT16_MAX) ? val - (2L << 15) : val);
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getInverterSerialNumber() {
        return inverterSerialNumber;
    }

    @Override
    public String getWifiSerialNumber() {
        return wifiSerialNumber;
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    @Override
    public BatteryMode getBatteryMode() {
        return batteryMode;
    }

    @Override
    public double getGrid1Voltage() {
        return grid1Voltage;
    }

    @Override
    public double getGrid2Voltage() {
        return grid2Voltage;
    }

    @Override
    public double getGrid3Voltage() {
        return grid3Voltage;
    }

    @Override
    public int getGrid1Power() {
        return grid1Power;
    }

    @Override
    public int getGrid2Power() {
        return grid2Power;
    }

    @Override
    public int getGrid3Power() {
        return grid3Power;
    }

    @Override
    public int getPv1Power() {
        return pv1Power;
    }

    @Override
    public int getPv2Power() {
        return pv2Power;
    }

    public int getEps1Power() {
        return eps1Power;
    }

    public int getEps2Power() {
        return eps2Power;
    }

    public int getEps3Power() {
        return eps3Power;
    }

    @Override
    public int getFeedInPower() {
        return feedInPower;
    }

    @Override
    public int getBatteryPower() {
        return batteryPower;
    }

    @Override
    public double getYieldTotal() {
        return yieldTotal;
    }

    @Override
    public double getYieldToday() {
        return yieldToday;
    }

    @Override
    public double getFeedInEnergyTotal() {
        return feedInEnergyTotal;
    }

    @Override
    public double getFeedInEnergyToday() {
        return feedInEnergyToday;
    }

    @Override
    public double getConsumedEnergyTotal() {
        return consumedEnergyTotal;
    }

    @Override
    public double getConsumedEnergyToday() {
        return consumedEnergyToday;
    }

    @Override
    public int getBatterySoc() {
        return batterySoc;
    }

    @Override
    public int getBatteryTemp() {
        return batteryTemp;
    }

    @Override
    public double getBatteryVoltage() {
        return batteryVoltage;
    }
}
