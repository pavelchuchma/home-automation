package org.chuma.homecontroller.extensions.external.inverter.impl;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.utils.Utils;
import org.chuma.homecontroller.extensions.external.inverter.InverterState;
import org.chuma.homecontroller.extensions.external.utils.ModbusClient;

/**
 * One-off experiment on the real inverter for doc/todo/006-inverter-control-via-vpp.md. Every method except
 * {@link #testStep1Snapshot()} WRITES to the inverter. Run the methods one at a time and in order, never the
 * whole class:
 * <pre>
 * ./gradlew :extensions:test --rerun -i --tests "*SolaxVppExperimentTest.testStep1Snapshot"
 * </pre>
 * Battery power is logged as the inverter reports it (positive charges); VPP targets have the opposite sign
 * (positive discharges). Feed-in is positive for export.
 */
public class SolaxVppExperimentTest extends AbstractSolaxInverterTestBase {
    private static final Logger log = LoggerFactory.getLogger(SolaxVppExperimentTest.class.getName());

    /**
     * Set to true once step 2 has shown that the firmware runs the VPP blocks. Until a run proves it by itself,
     * mode 0 is not sent: where the firmware does not know the VPP block, a single 0 at 0x00A0 would be a valid
     * stored MeterFunction = 0.
     */
    private static final boolean VPP_PROVEN_BY_OWNER = true;
    /**
     * PV target meaning "PV not limited"; must be at or above the PV peak power of the installation.
     */
    private static final int PV_NO_LIMIT_W = 10_000;
    private static final int PV_ABOVE_PEAK_W = 30_000;
    private static final int BATTERY_W = 2000;
    /**
     * SelfUse_Discharge_MinSoC left behind by step 6.
     */
    private static final int MIN_SOC_AFTER_STEP6 = 100;

    private static final int VPP_BLOCK_ADDRESS = 0x00A0;
    private static final int NEXT_MOTION_EXIT = 0x00A0;
    private static final int VPP_STATUS_START = 0x0100;
    private static final int[] VPP_STATUS_ENDS = {0x0121, 0x0101};
    private static final int METER_SETTINGS_START = 0x0108;
    private static final int METER_SETTINGS_END = 0x010D;

    private SolaxInverterModbusClient inverter;
    private long startTime;
    // last readable register of the VPP status block: -1 not tried yet, 0 unreadable
    private int vppStatusEnd = -1;
    private int[] meterSettingsBefore;

    private boolean vppProven = VPP_PROVEN_BY_OWNER;
    private boolean modeStarted;
    private boolean firmwareTookBlockAsSettings;
    private int commandedMode;
    // battery power expected in the samples (positive charges), null when the mode sets no battery target
    private Integer expectedBatteryW;
    private int baselineBatteryW;
    private int samplesNearTarget;
    private int lastVppMode = -1;

    @Override
    protected void setUp() throws Exception {
        inverter = new SolaxInverterModbusClient(localIp);
        startTime = System.currentTimeMillis();
    }

    /**
     * Step 1: read only.
     */
    public void testStep1Snapshot() {
        snapshot();
        observe(30, 5);
    }

    /**
     * Read only: waits up to 9 minutes until the inverter runs in the Normal mode.
     */
    public void testWaitForNormal() {
        for (int i = 0; i < 54; i++) {
            Sample sample = observe(10, 10);
            if (sample != null && sample.runMode.equals(InverterState.Mode.Normal.name())) {
                return;
            }
        }
        fail("The inverter did not reach the Normal mode");
    }

    /**
     * Steps 2 and 3: charge 2 kW for 120 s, then send nothing. Is the block accepted, does the battery charge,
     * does 0x0100 show 8, and does the mode end by itself after the duration? Run it in the Normal run mode:
     * from Idle the inverter needs about 330 s to start charging, longer than this command lasts.
     */
    public void testStep2ChargeAndStep3Expire() {
        run(() -> {
            startPvBattery(PV_NO_LIMIT_W, -BATTERY_W, 120);
            observe(240, 5);
        });
    }

    /**
     * Step 4: the same command re-issued every 60 s. Does the duration restart (the mode then ends 120 s after
     * the last command), and does the battery power dip at a re-issue?
     */
    public void testStep4Reissue() {
        run(() -> {
            startPvBattery(PV_NO_LIMIT_W, -BATTERY_W, 120);
            for (int i = 0; i < 3; i++) {
                observe(60, 2);
                startPvBattery(PV_NO_LIMIT_W, -BATTERY_W, 120);
            }
            observe(180, 5);
        });
    }

    /**
     * Step 5: mode 0 while a mode runs. Does the inverter return to the stored work mode at once?
     */
    public void testStep5DisableWhileRunning() {
        run(() -> {
            startPvBattery(PV_NO_LIMIT_W, -BATTERY_W, 600);
            observe(40, 2);
            if (!vppProven) {
                log.warn("VPP mode not proven to run, mode 0 is not sent");
                return;
            }
            log.info("{} WRITE mode 0", elapsed());
            inverter.vppDisable();
            observe(60, 2);
        });
    }

    /**
     * Step 6: discharge 2 kW. Does the discharge stop at SelfUse_Discharge_MinSoC? To keep the run short it
     * WRITES that stored setting: SoC - 1 % before the run and {@link #MIN_SOC_AFTER_STEP6} after it. The run
     * ends itself 2 % below the min SoC. The export cap shows only when ExportControl_User_Limit is below the
     * discharge power minus the house load, so lower the limit beforehand; this test does not write it. By
     * day the log also shows whether PV stays unlimited.
     */
    public void testStep6DischargeToMinSoc() {
        int minSoc = inverter.getState().getBatterySoc() - 1;
        log.info("WRITE SelfUse_Discharge_MinSoC = {} %", minSoc);
        inverter.setSelfUseMinimalSoc(minSoc);
        try {
            run(() -> {
                startPvBattery(PV_NO_LIMIT_W, BATTERY_W, 900);
                int chunksWithoutDischarge = 0;
                for (int i = 0; i < 32 && chunksWithoutDischarge < 3; i++) {
                    Sample sample = observe(30, 5);
                    if (sample == null) {
                        continue;
                    }
                    if (sample.soc < minSoc - 2) {
                        log.warn("SoC {} % is below min SoC - 2 %, the discharge did not stop by itself", sample.soc);
                        return;
                    }
                    chunksWithoutDischarge = sample.batteryW > -BATTERY_W / 2 ? chunksWithoutDischarge + 1 : 0;
                }
            });
        } finally {
            log.info("WRITE SelfUse_Discharge_MinSoC = {} %", MIN_SOC_AFTER_STEP6);
            inverter.setSelfUseMinimalSoc(MIN_SOC_AFTER_STEP6);
        }
    }

    /**
     * Step 6, second part: is a PV target far above the PV peak accepted?
     */
    public void testStep6PvTargetAbovePeak() {
        run(() -> {
            startPvBattery(PV_ABOVE_PEAK_W, -BATTERY_W, 60);
            observe(120, 5);
        });
    }

    /**
     * Step 7, by day: PV off and the battery charged from the grid for 300 s. How long does PV take to go down,
     * and to come back after the mode ends? Run it at SoC 100 % as well.
     */
    public void testStep7PvOffGridCharge() {
        run(() -> {
            startPvBattery(0, -BATTERY_W, 300);
            observe(600, 5);
        });
    }

    /**
     * Step 8, optional: mode 4 (push power) through 0x00A0, discharge 2 kW for 120 s.
     */
    public void testStep8PushPower() {
        run(() -> {
            start("mode 4 push power", 4, -BATTERY_W,
                    new int[]{4, BATTERY_W & 0xFFFF, BATTERY_W >>> 16, 120, NEXT_MOTION_EXIT});
            observe(180, 5);
        });
    }

    /**
     * Step 9, for todo 005: mode 7 (self-consume, charge only) for 60 s with the offsets from SolaX's tables.
     * The two layouts use different durations, and neither the 120 s of the other steps: a block shorter than
     * the previous one may leave the earlier values in the registers behind it.
     */
    public void testStep9ChargeOnlyTableLayout() {
        run(() -> {
            start("mode 7, duration at 0xA1", 7, null, new int[]{7, 60, NEXT_MOTION_EXIT});
            observe(180, 5);
        });
    }

    /**
     * Step 9: mode 7 for 90 s with the offsets from SolaX's examples.
     */
    public void testStep9ChargeOnlyExampleLayout() {
        run(() -> {
            start("mode 7, duration at 0xA3", 7, null, new int[]{7, 0, 0, 90, NEXT_MOTION_EXIT});
            observe(210, 5);
        });
    }

    /**
     * Step 9, for todo 005: charge 2 kW for 600 s while the inverter is Idle. Does the command wake it
     * (VPPExitIdleEn), and how long does it take to start charging?
     */
    public void testStep9ChargeFromIdle() {
        run(() -> {
            startPvBattery(PV_NO_LIMIT_W, -BATTERY_W, 600);
            observe(900, 5);
        });
    }

    /**
     * Ends a VPP mode left running. Only once step 2 has shown that the firmware runs the VPP blocks, see
     * {@link #VPP_PROVEN_BY_OWNER}.
     */
    public void testVppDisable() {
        snapshot();
        inverter.vppDisable();
        observe(60, 5);
    }

    private void run(Runnable body) {
        snapshot();
        try {
            body.run();
        } finally {
            finish();
        }
    }

    private void startPvBattery(int pvLimitW, int batteryW, int durationS) {
        start("mode 8, PV " + pvLimitW + " W, battery " + batteryW + " W, " + durationS + " s", 8, -batteryW,
                () -> inverter.vppPvBattery(pvLimitW, batteryW, durationS));
    }

    private void start(String description, int mode, Integer expectedBatteryW, int[] block) {
        start(description + ": " + hex(block), mode, expectedBatteryW, () -> {
            try {
                noRangesClient().writeMultipleRegisterValues(VPP_BLOCK_ADDRESS, block);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Sends a VPP command. The first command of a run is guarded: it is sent only from a known state and the
     * stored meter settings are compared afterwards.
     */
    private void start(String description, int mode, Integer expectedBatteryW, Runnable write) {
        boolean first = !modeStarted;
        if (first) {
            if (meterSettingsBefore == null) {
                throw new IllegalStateException("Meter settings 0x0108-0x010D are not readable, cannot detect a block taken as stored settings");
            }
            Sample sample = sample();
            if (sample.batteryMode == InverterState.BatteryMode.Manual) {
                throw new IllegalStateException("Work mode is Manual, a VPP mode must not be started from it");
            }
            baselineBatteryW = sample.batteryW;
        }
        commandedMode = mode;
        this.expectedBatteryW = expectedBatteryW;
        samplesNearTarget = 0;
        log.info("{} WRITE {}", elapsed(), description);
        // a rejected write throws and leaves modeStarted unset
        write.run();
        modeStarted = true;
        if (first) {
            Utils.sleep(2000);
            checkMeterSettings();
        }
    }

    private void finish() {
        if (!modeStarted || firmwareTookBlockAsSettings) {
            return;
        }
        try {
            checkMeterSettings();
        } catch (RuntimeException e) {
            log.error("Meter settings check failed, mode 0 is not sent", e);
            return;
        }
        if (!vppProven) {
            log.warn("No sign that a VPP mode ran (0x0100 never showed mode {} and the battery did not follow the target),"
                    + " mode 0 is NOT sent. Check the inverter.", commandedMode);
            return;
        }
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                log.info("{} WRITE mode 0 (cleanup)", elapsed());
                inverter.vppDisable();
                observe(20, 5);
                return;
            } catch (RuntimeException e) {
                log.error("Cannot send mode 0, attempt {}", attempt, e);
                Utils.sleep(2000);
            }
        }
        log.error("VPP MODE MAY STILL BE RUNNING, run testVppDisable");
    }

    private void snapshot() {
        InverterState state = inverter.getState();
        log.info("version: {}", state.getVersion());
        log.info("run mode {}, work mode {}, manual mode {}, SoC {} %, min SoC {} %, export limit {} W",
                runMode(state), state.getBatteryMode(), state.getManualMode(), state.getBatterySoc(),
                state.getSelfUseMinimalSoc(), state.getExportControlUserLimit());
        log.info("VPP supported: {}, VPP mode polled by the client: {}", inverter.isVppSupported(), state.getVppMode());
        log.info("input   0x0054 LockState (0 locked, 1 unlocked): {}", hex(read(true, 0x0054, 0x0054)));
        log.info("holding 0x00B4 VPPExitIdleEn: {}", hex(read(false, 0x00B4, 0x00B4)));
        meterSettingsBefore = read(false, METER_SETTINGS_START, METER_SETTINGS_END);
        log.info("holding 0x0108-0x010D MeterFunction, Meter1ID, Meter2ID, DirectionMeterCT1, DirectionMeter2, ExternalInv: {}",
                hex(meterSettingsBefore));
        int[] vppStatus = readVppStatus();
        log.info("input   0x0100-0x{} VPP status: {}", String.format("%04X", vppStatusEnd), hex(vppStatus));
    }

    private void checkMeterSettings() {
        int[] now = null;
        for (int attempt = 0; now == null && attempt < 3; attempt++) {
            now = read(false, METER_SETTINGS_START, METER_SETTINGS_END);
        }
        if (now == null) {
            throw new IllegalStateException("Meter settings 0x0108-0x010D are not readable any more");
        }
        if (!Arrays.equals(meterSettingsBefore, now)) {
            firmwareTookBlockAsSettings = true;
            throw new IllegalStateException("STORED METER SETTINGS CHANGED, the firmware took the VPP block as settings."
                    + " Holding 0x0108-0x010D before: " + hex(meterSettingsBefore) + ", now: " + hex(now)
                    + ". Restore them (FC 0x06, 0x00A0-0x00A7); nothing more is written.");
        }
    }

    /**
     * Logs a sample every {@code intervalS} seconds, returns the last one (null when no read succeeded).
     */
    private Sample observe(int seconds, int intervalS) {
        Sample last = null;
        long end = System.currentTimeMillis() + seconds * 1000L;
        while (System.currentTimeMillis() < end) {
            try {
                last = sample();
                log.info("{} {}", elapsed(), last);
                trackVppMode(last);
            } catch (RuntimeException e) {
                log.warn("{} sample failed: {}", elapsed(), e.toString());
            }
            Utils.sleep(intervalS * 1000L);
        }
        return last;
    }

    private void trackVppMode(Sample sample) {
        if (sample.vppMode != lastVppMode) {
            if (lastVppMode >= 0) {
                log.info("{} VPP status 0x0100 changed {} -> {}", elapsed(), lastVppMode, sample.vppMode);
            }
            lastVppMode = sample.vppMode;
        }
        if (vppProven || !modeStarted) {
            return;
        }
        if (sample.vppMode == commandedMode) {
            vppProven = true;
            log.info("{} VPP proven: 0x0100 shows mode {}", elapsed(), commandedMode);
            return;
        }
        // the battery counts only when it had to move at least 800 W to reach the target
        if (expectedBatteryW == null || Math.abs(baselineBatteryW - expectedBatteryW) < 800) {
            return;
        }
        samplesNearTarget = Math.abs(sample.batteryW - expectedBatteryW) < 400 ? samplesNearTarget + 1 : 0;
        if (samplesNearTarget >= 2) {
            vppProven = true;
            log.info("{} VPP proven: the battery follows the target", elapsed());
        }
    }

    private Sample sample() {
        InverterState state = inverter.getState();
        int[] vpp = readVppStatus();
        boolean full = vpp != null && vpp.length > 0x1E;
        return new Sample(runMode(state), state.getBatteryMode(), state.getManualMode(),
                vpp != null ? vpp[0] : -1,
                vpp != null ? vpp[1] : -1,
                full ? (vpp[0x15] << 16) | vpp[0x14] : 0,
                full ? vpp[0x1A] : -1,
                full ? vpp[0x1E] : -1,
                state.getBatteryPower(), state.getPv1Power() + state.getPv2Power(), state.getFeedInPower(),
                state.getBatterySoc());
    }

    /**
     * Run mode name, or the raw register value for a mode missing in {@link InverterState.Mode}.
     */
    private String runMode(InverterState state) {
        InverterState.Mode mode = state.getMode();
        return mode == InverterState.Mode.Unknown ? "#" + hex(read(true, 0x0009, 0x0009)) : mode.name();
    }

    /**
     * Legacy VPP status block, as much of it as the inverter lets read; null when none.
     */
    private int[] readVppStatus() {
        if (vppStatusEnd < 0) {
            vppStatusEnd = 0;
            for (int end : VPP_STATUS_ENDS) {
                int[] res = read(true, VPP_STATUS_START, end);
                if (res != null) {
                    vppStatusEnd = end;
                    return res;
                }
            }
        }
        return vppStatusEnd == 0 ? null : read(true, VPP_STATUS_START, vppStatusEnd);
    }

    /**
     * Reads registers outside the polled ranges; null when the inverter rejects the read.
     */
    private int[] read(boolean inputRegisters, int from, int to) {
        int[][] range = {{from, to}};
        int[][] none = {};
        try {
            ModbusClient client = new ModbusClient(localIp, 502, true, 3000,
                    inputRegisters ? range : none, inputRegisters ? none : range, none);
            client.refreshData();
            ModbusClient.Registers registers = inputRegisters ? client.input : client.holding;
            int[] res = new int[to - from + 1];
            for (int i = 0; i < res.length; i++) {
                res[i] = registers.getUnsignedInt(from + i);
            }
            return res;
        } catch (Exception e) {
            log.warn("cannot read {} registers 0x{}-0x{}: {}", inputRegisters ? "input" : "holding",
                    String.format("%04X", from), String.format("%04X", to), e.toString());
            return null;
        }
    }

    private ModbusClient noRangesClient() throws Exception {
        int[][] none = {};
        return new ModbusClient(localIp, 502, true, 3000, none, none, none);
    }

    private String elapsed() {
        return String.format("t=+%ds", (System.currentTimeMillis() - startTime) / 1000);
    }

    private static String hex(int[] values) {
        if (values == null) {
            return "unreadable";
        }
        StringBuilder res = new StringBuilder();
        for (int value : values) {
            res.append(String.format("%04X ", value));
        }
        return res.toString().trim();
    }

    /**
     * @param vppMode       input 0x0100 ModbusPowerControl, -1 when unreadable
     * @param vppFinished   input 0x0101 TargetFinishFlag
     * @param vppBatteryW   input 0x0114 Charge_Discharg_Power
     * @param vppDurationS  input 0x011A Time_of_Duration
     * @param vppTimeoutS   input 0x011E RemoteCtrlTimeOut
     * @param batteryW      positive charges
     * @param feedInW       positive exports
     */
    private record Sample(String runMode, InverterState.BatteryMode batteryMode,
                          InverterState.ManualMode manualMode, int vppMode, int vppFinished, int vppBatteryW,
                          int vppDurationS, int vppTimeoutS, int batteryW, int pvW, int feedInW, int soc) {
        @Override
        public String toString() {
            return String.format("run=%s work=%s/%s vpp=%d finished=%d vppBat=%dW vppDur=%ds vppTimeout=%ds"
                            + " | bat=%dW pv=%dW feedIn=%dW soc=%d%%",
                    runMode, batteryMode, manualMode, vppMode, vppFinished, vppBatteryW, vppDurationS, vppTimeoutS,
                    batteryW, pvW, feedInW, soc);
        }
    }
}
