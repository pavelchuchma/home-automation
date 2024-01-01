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
     * Total AC Power in W (Inverter output to house excluding battery)
     */
    default int getGridPower() {
        return getGrid1Power() + getGrid2Power() + getGrid3Power();
    }

    int getGrid1Power();

    int getGrid2Power();

    int getGrid3Power();

    /**
     * Total EPS Power in W (Inverter output to EPS (backup))
     */
    default int getEpsPower() {
        return getEps1Power() + getEps2Power() + getEps3Power();
    }

    int getEps1Power();

    int getEps2Power();

    int getEps3Power();

    /**
     * Total Photovoltaic power
     */
    default int getPvPower() {
        return getPv1Power() + getPv2Power();
    }

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

    int getSelfUseMinimalSoc();

    PgridBias getPgridBias();

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

    enum PgridBias {
        /**
         * / Targets FeedInPower to ~0W
         */
        Disable,
        /**
         * Targets FeedInPower to ~+40W
         */
        Grid,
        /**
         * Targets FeedInPower to ~-40W
         */
        Inv
    }
}
