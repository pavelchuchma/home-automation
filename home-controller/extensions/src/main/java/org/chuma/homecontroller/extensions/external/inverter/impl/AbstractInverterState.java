package org.chuma.homecontroller.extensions.external.inverter.impl;

import org.chuma.homecontroller.extensions.external.inverter.InverterState;

public abstract class AbstractInverterState implements InverterState {
    public final long timestamp = System.currentTimeMillis();

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "State{" +
                "\n  version='" + getVersion() + '\'' +
                "\n  inverterSerialNumber='" + getInverterSerialNumber() + '\'' +
                "\n  wifiSerialNumber='" + getWifiSerialNumber() + '\'' +
                "\n  mode=" + getMode() +
                "\n  batteryMode=" + getBatteryMode() +
                "\n  grid1Voltage=" + getGrid1Voltage() +
                "\n  grid2Voltage=" + getGrid2Voltage() +
                "\n  grid3Voltage=" + getGrid3Voltage() +
                "\n  grid1Power=" + getGrid1Power() +
                "\n  grid2Power=" + getGrid2Power() +
                "\n  grid3Power=" + getGrid3Power() +
                "\n  pv1Power=" + getPv1Power() +
                "\n  pv2Power=" + getPv2Power() +
                "\n  eps1Power=" + getEps1Power() +
                "\n  eps2Power=" + getEps2Power() +
                "\n  eps3Power=" + getEps3Power() +
                "\n  feedInPower=" + getFeedInPower() +
                "\n  batteryPower=" + getBatteryPower() +
                "\n  yieldTotal=" + getYieldTotal() +
                "\n  yieldToday=" + getYieldToday() +
                "\n  feedInEnergyTotal=" + getFeedInEnergyTotal() +
                "\n  feedInEnergyToday=" + getFeedInEnergyToday() +
                "\n  consumedEnergyTotal=" + getConsumedEnergyTotal() +
                "\n  consumedEnergyToday=" + getConsumedEnergyToday() +
                "\n  batterySoc=" + getBatterySoc() +
                "\n  batteryTemp=" + getBatteryTemp() +
                "\n  batteryVoltage=" + getBatteryVoltage() +
                "\n  selfUseMinimalSoc=" + getSelfUseMinimalSoc() +
                "\n  pgridBias=" + getPgridBias() +
                "\n}";
    }
}
