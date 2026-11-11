package org.chuma.homecontroller.extensions.external.inverter;

public interface InverterState {
    long getTimestamp();

    String getVersion();

    String getInverterSerialNumber();

    String getWifiSerialNumber();

    Mode getMode();

    BatteryMode getBatteryMode();

    ManualMode getManualMode();

    /**
     * Number of the running VPP (remote control) mode, see doc/inverter/solax_vpp_protocol.md;
     * 0 when none runs or the inverter does not support VPP.
     */
    int getVppMode();

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

    /**
     * PV string 1 DC voltage
     */
    double getPv1Voltage();

    int getPv2Power();

    double getPv2Voltage();

    /**
     * Positive value means feeding energy to the public grid
     */
    int getFeedInPower();

    /**
     * Feed-in power on phase 1 measured by the grid meter; positive = export, negative = import
     */
    int getFeedIn1Power();

    int getFeedIn2Power();

    int getFeedIn3Power();

    /**
     * Positive value means battery charging
     */
    int getBatteryPower();

    double getYieldTotal();

    double getYieldToday();

    double getPvYieldToday();

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

    /**
     * Export control user limit in W
     */
    int getExportControlUserLimit();

    enum Mode {
        Waiting(0),
        Checking(1),
        Normal(2),
        Fault(3),
        PermanentFault(4),
        Updating(5),
        EPSCheck(6),
        EPSMode(7),
        SelfTest(8),
        Idle(9),
        Standby(10),
        /**
         * Running under a VPP (remote control) mode.
         */
        RemoteControl(20),
        /**
         * A run mode code this enum does not know.
         */
        Unknown(-1);

        private final int code;

        Mode(int code) {
            this.code = code;
        }

        /**
         * @param code value of the Solax RunMode register (read 0x0009)
         */
        public static Mode fromCode(int code) {
            for (Mode mode : values()) {
                if (mode.code == code) {
                    return mode;
                }
            }
            return Unknown;
        }

        /**
         * The inverter is on the grid and converting power.
         */
        public boolean isRunning() {
            return this == Normal || this == RemoteControl;
        }
    }

    enum BatteryMode {
        SelfUse,
        FeedInPriority,
        BackUp,
        Manual
    }

    /**
     * Behavior of the inverter while {@link BatteryMode#Manual} is active.
     * Ordinals match the Solax ManualMode register (write 0x0020, read 0x008C).
     */
    enum ManualMode {
        Stop,
        ForceCharge,
        ForceDischarge
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
