package org.chuma.homecontroller.extensions.external.inverter.impl;

import java.net.UnknownHostException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.extensions.external.inverter.InverterState;
import org.chuma.homecontroller.extensions.external.utils.ModbusClient;

/**
 * Client for local Modbus API of "Solax X3-Hybrid G4 Inverter"
 * Modbus specification: doc/inverter/solax_modbus_spec_3.34.md
 */
public class SolaxInverterModbusClient {
    protected static Logger log = LoggerFactory.getLogger(SolaxInverterModbusClient.class.getName());
    // VPP blocks are written with FC 0x10 only; FC 0x06 has unrelated stored settings at this address
    private static final int VPP_BLOCK_ADDRESS = 0x00A0;
    private static final int VPP_MODE_PV_BATTERY = 8;
    // a new command restarts the target instead of updating the running one
    private static final int VPP_SET_TYPE_RESET = 1;
    // after the duration leave VPP and return to the stored work mode
    private static final int VPP_NEXT_MOTION_EXIT = 0x00A0;
    private static final int VPP_STATUS_REGISTER = 0x0100;
    private static final int VPP_STATUS_ATTEMPTS = 3;
    private static final int ARM_VERSION_MAJOR_REGISTER = 0x0080;
    private static final int ARM_VERSION_MINOR_REGISTER = 0x0083;
    private final String host;
    private final Set<Integer> reportedUnknownRunModes = ConcurrentHashMap.newKeySet();
    private volatile ModbusClient client;
    // the VPP status register is polled only once the firmware is known to have it
    private volatile boolean vppStatusPolled;
    private int vppStatusFailures;

    public SolaxInverterModbusClient(String host) throws UnknownHostException {
        this.host = host;
        client = createClient(false);
    }

    private ModbusClient createClient(boolean withVppStatus) throws UnknownHostException {
        int[][] inputRanges = withVppStatus
                ? new int[][]{{0x0003, 0x0053}, {0x006A, 0x009B}, {VPP_STATUS_REGISTER, VPP_STATUS_REGISTER}}
                : new int[][]{{0x0003, 0x0053}, {0x006A, 0x009B}};
        return new ModbusClient(host, 502, true, 3000,
                inputRanges,
                new int[][]{{0x007D, 0x0093}, {0x00B2, 0x00B6}},
                new int[][]{{0x0000, 0x0006}}
        );
    }

    public InverterState getState() {
        client.refreshData();
        if (!vppStatusPolled) {
            startVppStatusPolling();
        }
        return new State();
    }

    /**
     * Adds the VPP status register to the polled ones. An inverter without VPP may reject the whole read,
     * so it is added only for a firmware that has VPP and dropped again when the read keeps failing.
     */
    private synchronized void startVppStatusPolling() {
        if (vppStatusPolled || vppStatusFailures >= VPP_STATUS_ATTEMPTS || !isVppSupported()) {
            return;
        }
        try {
            ModbusClient extended = createClient(true);
            extended.refreshData();
            client = extended;
            vppStatusPolled = true;
        } catch (Exception e) {
            vppStatusFailures++;
            log.warn("Cannot read the VPP status register (attempt {} of {})", vppStatusFailures, VPP_STATUS_ATTEMPTS, e);
        }
    }

    /**
     * Whether the firmware has the VPP protocol at 0x00A0: ARM 1.45 or newer, the requirement of
     * X3-Hybrid-G4. Other models are not told apart.
     */
    public synchronized boolean isVppSupported() {
        if (client.holding.getUnsignedInt(ARM_VERSION_MAJOR_REGISTER) == Integer.MIN_VALUE) {
            client.refreshData();
        }
        return isVppFirmware(client.holding.getUnsignedInt(ARM_VERSION_MAJOR_REGISTER),
                client.holding.getUnsignedInt(ARM_VERSION_MINOR_REGISTER));
    }

    static boolean isVppFirmware(int armMajor, int armMinor) {
        return armMajor > 1 || (armMajor == 1 && armMinor >= 45);
    }

    private void requireVpp() {
        if (!isVppSupported()) {
            int major = client.holding.getUnsignedInt(ARM_VERSION_MAJOR_REGISTER);
            String version = major == Integer.MIN_VALUE ? "unknown"
                    : major + "." + client.holding.getUnsignedInt(ARM_VERSION_MINOR_REGISTER);
            throw new UnsupportedOperationException(
                    "The inverter does not support VPP control: it needs ARM firmware 1.45 or newer, this one has " + version);
        }
    }

    /**
     * Reads 2 registers wide int from the specified and following registers.
     */
    private static int getWideInt(ModbusClient.Registers registers, int lsbIndex) {
        return (int)(registers.getUnsignedInt(lsbIndex) + ((long)registers.getUnsignedInt(lsbIndex + 1) << 16));
    }

    public synchronized void setInverterOn(boolean on) {
        log.info("setInverterOn({})", on);
        client.writeSingleRegisterValue(0x001C, on ? 1 : 0);
    }

    public synchronized void setSelfUseMinimalSoc(int value) {
        log.debug("setSelfUseMinimalSoc({})", value);
        Validate.inclusiveBetween(10, 100, value);
        client.writeSingleRegisterValue(0x0061, value);
    }

    /**
     * @param value export limit in W (X3 register unit is 10W)
     */
    public synchronized void setExportControlUserLimit(int value) {
        log.debug("setExportControlUserLimit({})", value);
        Validate.inclusiveBetween(0, 600000, value);
        client.writeSingleRegisterValue(0x0042, value / 10);
    }

    public synchronized void setPgridBias(InverterState.PgridBias value) {
        log.debug("setPgridBias({})", value);
        client.writeSingleRegisterValue(0x008D, value.ordinal());
    }

    public synchronized void setBatteryMode(InverterState.BatteryMode value) {
        log.info("setBatteryMode({})", value);
        client.writeSingleRegisterValue(0x001F, value.ordinal());
    }

    public synchronized void setManualMode(InverterState.ManualMode value) {
        log.info("setManualMode({})", value);
        client.writeSingleRegisterValue(0x0020, value.ordinal());
    }

    /**
     * Ends a running VPP mode; the inverter returns to its stored work mode.
     *
     * @throws UnsupportedOperationException when the inverter firmware has no VPP
     */
    public synchronized void vppDisable() {
        log.info("vppDisable()");
        requireVpp();
        client.writeMultipleRegisterValues(VPP_BLOCK_ADDRESS, new int[]{0});
    }

    /**
     * Starts VPP mode 8 (PV and battery controlled individually), see doc/inverter/solax_vpp_protocol.md.
     *
     * @param pvLimitW  PV power limit in W, 0 turns PV off
     * @param batteryW  battery power in W, positive discharges, negative charges
     * @param durationS seconds after which the inverter returns to its stored work mode
     * @throws UnsupportedOperationException when the inverter firmware has no VPP
     */
    public synchronized void vppPvBattery(int pvLimitW, int batteryW, int durationS) {
        log.info("vppPvBattery(pvLimitW={}, batteryW={}, durationS={})", pvLimitW, batteryW, durationS);
        requireVpp();
        client.writeMultipleRegisterValues(VPP_BLOCK_ADDRESS, buildVppPvBatteryBlock(pvLimitW, batteryW, durationS));
    }

    /**
     * Registers of the VPP mode 8 block; 32-bit values have the low word first.
     */
    static int[] buildVppPvBatteryBlock(int pvLimitW, int batteryW, int durationS) {
        Validate.isTrue(pvLimitW >= 0, "Negative PV limit: %d", pvLimitW);
        Validate.inclusiveBetween(1, 0xFFFF, durationS);
        return new int[]{
                VPP_MODE_PV_BATTERY,
                VPP_SET_TYPE_RESET,
                pvLimitW & 0xFFFF, pvLimitW >>> 16,
                batteryW & 0xFFFF, batteryW >>> 16,
                durationS,
                VPP_NEXT_MOTION_EXIT
        };
    }

    public class State extends AbstractInverterState {
        @Override
        public String getVersion() {
            return "DSP: " + client.holding.getUnsignedInt(0x007F) + '.' + client.holding.getUnsignedInt(0x007D)
                    + "; ARM: " + client.holding.getUnsignedInt(0x0080) + '.' + client.holding.getUnsignedInt(0x0083)
                    + "; ARM-BL: " + client.holding.getUnsignedInt(0x0084)
                    + "; ModbusRTU: " + client.holding.getUnsignedInt(0x0082)
                    + "; HW: " + client.holding.getUnsignedInt(0x007E);
        }

        @Override
        public String getInverterSerialNumber() {
            return client.holding.getString(0x0000, 7);
        }

        @Override
        public String getWifiSerialNumber() {
            return "???";
        }

        @Override
        public Mode getMode() {
            int code = client.input.getUnsignedInt(0x0009);
            Mode mode = Mode.fromCode(code);
            if (mode == Mode.Unknown && reportedUnknownRunModes.add(code)) {
                log.warn("Unknown inverter run mode {}", code);
            }
            return mode;
        }

        @Override
        public int getVppMode() {
            return vppStatusPolled ? client.input.getUnsignedInt(VPP_STATUS_REGISTER) : 0;
        }

        @Override
        public BatteryMode getBatteryMode() {
            return BatteryMode.values()[client.holding.getUnsignedInt(0x008B)];
        }

        @Override
        public ManualMode getManualMode() {
            return ManualMode.values()[client.holding.getUnsignedInt(0x008C)];
        }

        @Override
        public double getGrid1Voltage() {
            return client.input.getUnsignedInt(0x006A) / 10d;
        }

        @Override
        public double getGrid2Voltage() {
            return client.input.getUnsignedInt(0x006E) / 10d;
        }

        @Override
        public double getGrid3Voltage() {
            return client.input.getUnsignedInt(0x0072) / 10d;
        }

        @Override
        public int getGrid1Power() {
            return client.input.getSignedInt(0x006C);
        }

        @Override
        public int getGrid2Power() {
            return client.input.getSignedInt(0x0070);
        }

        @Override
        public int getGrid3Power() {
            return client.input.getSignedInt(0x0074);
        }

        @Override
        public int getPv1Power() {
            return client.input.getUnsignedInt(0x000A);
        }

        @Override
        public double getPv1Voltage() {
            return client.input.getUnsignedInt(0x0003) / 10d;
        }

        @Override
        public int getPv2Power() {
            return client.input.getUnsignedInt(0x000B);
        }

        @Override
        public double getPv2Voltage() {
            return client.input.getUnsignedInt(0x0004) / 10d;
        }

        public int getEps1Power() {
            return client.input.getSignedInt(0x0078);
        }

        public int getEps2Power() {
            return client.input.getSignedInt(0x007C);
        }

        public int getEps3Power() {
            return client.input.getSignedInt(0x0080);
        }

        @Override
        public int getFeedInPower() {
            return getWideInt(client.input, 0x0046);
        }

        @Override
        public int getFeedIn1Power() {
            return getWideInt(client.input, 0x0082);
        }

        @Override
        public int getFeedIn2Power() {
            return getWideInt(client.input, 0x0084);
        }

        @Override
        public int getFeedIn3Power() {
            return getWideInt(client.input, 0x0086);
        }

        @Override
        public int getBatteryPower() {
            return client.input.getSignedInt(0x0016);
        }

        @Override
        public double getYieldTotal() {
            return getWideInt(client.input, 0x0052) / 10d;
        }

        @Override
        public double getYieldToday() {
            return client.input.getUnsignedInt(0x0050) / 10d;
        }

        @Override
        public double getPvYieldToday() {
            return client.input.getUnsignedInt(0x0096) / 10d;
        }

        @Override
        public double getFeedInEnergyTotal() {
            return getWideInt(client.input, 0x0048) / 100d;
        }

        @Override
        public double getFeedInEnergyToday() {
            return getWideInt(client.input, 0x0098) / 100d;
        }

        @Override
        public double getConsumedEnergyTotal() {
            return getWideInt(client.input, 0x004A) / 100d;
        }

        @Override
        public double getConsumedEnergyToday() {
            return getWideInt(client.input, 0x009A) / 100d;
        }

        @Override
        public int getBatterySoc() {
            return client.input.getUnsignedInt(0x001C);
        }

        @Override
        public int getBatteryTemp() {
            return client.input.getUnsignedInt(0x0018);
        }

        @Override
        public double getBatteryVoltage() {
            return client.input.getSignedInt(0x0014) / 10d;
        }

        @Override
        public int getSelfUseMinimalSoc() {
            return (client.holding.getUnsignedInt(0x0093) & 0xFF00) >> 8;
        }

        @Override
        public PgridBias getPgridBias() {
            return PgridBias.values()[client.holding.getUnsignedInt(0x00B2)];
        }

        @Override
        public int getExportControlUserLimit() {
            return client.holding.getUnsignedInt(0x00B6) * 10;
        }
    }
}
