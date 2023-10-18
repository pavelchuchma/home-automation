package org.chuma.homecontroller.extensions.external.inverter;

public interface InverterState {
    long getTimestamp();

    String getVersion();

    String getInverterSerialNumber();

    String getWifiSerialNumber();

    Mode getMode();

    BatteryMode getBatteryMode();

    /**
     * Voltage on AC Power, phase 1
     */
    double getGrid1Voltage();

    double getGrid2Voltage();

    double getGrid3Voltage();

    /**
     * AC Power in W (Inverter output to house excluding battery), phase 1
     */
    int getGrid1Power();

    int getGrid2Power();

    int getGrid3Power();

    /**
     * EPS Power in W (Inverter output to EPS (backup)), phase 1
     */
    int getEps1Power();
    int getEps2Power();
    int getEps3Power();

    /**
     * Photovoltaic power, panel 1
     */
    int getPv1Power();

    int getPv2Power();

    /**
     * Positive value means feeding energy to the public grid
     */
    int getFeedInPower();

    /**
     * Positive value means battery charging
     */
    int getBatteryPower();

    double getYieldTotal();

    double getYieldToday();

    double getFeedInEnergyTotal();

    double getFeedInEnergyToday();

    double getConsumedEnergyTotal();

    double getConsumedEnergyToday();

    /**
     * Battery state of charge in percent
     */
    int getBatterySoc();

    /**
     * Battery temperature in C
     */
    int getBatteryTemp();

    double getBatteryVoltage();

    enum Mode {
        Waiting,
        Checking,
        Normal,
        Fault,
        PermanentFault,
        Updating,
        EPSCheck,
        EPSMode,
        SelfTest,
        Idle,
        Standby
    }

    enum BatteryMode {
        SelfUse,
        ForceTime,
        BackUp,
        FeedInPriority
    }
}
