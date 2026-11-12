package org.chuma.homecontroller.extensions.external.inverter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.utils.Options;
import org.chuma.homecontroller.extensions.external.inverter.ElectricitySpotPriceMonitor.IntervalPrice;
import org.chuma.homecontroller.extensions.external.inverter.impl.SolaxInverterModbusClient;
import org.chuma.homecontroller.extensions.external.utils.IntervalScheduler;

public class InverterManager {
    private static final String CFG_INVERTER_MANAGER_HIGH_TARIFF_BATTERY_RESERVE = "inverter.manager.high.tariff.battery.reserve";
    private static final String CFG_INVERTER_MANAGER_HIGH_TARIFF_TIMES = "inverter.manager.high.tariff.times";
    private static final String CFG_INVERTER_MANAGER_MINIMAL_SOC = "inverter.manager.minimal.soc";
    private static final String CFG_INVERTER_MANAGER_TURNOFF_ON_NEGATIVE_PRICE = "inverter.manager.turnoff.on.negative.price";
    private static final String CFG_INVERTER_MANAGER_EXPORT_PRIORITY_TIMER_ENABLED = "inverter.manager.export.priority.timer.enabled";
    private static final String CFG_INVERTER_MANAGER_EXPORT_PRIORITY_TIMES = "inverter.manager.export.priority.times";
    private static final String CFG_INVERTER_MANAGER_FORCE_DISCHARGE = "inverter.manager.force.discharge";
    private static final String CFG_INVERTER_MANAGER_FORCE_DISCHARGE_MINIMAL_PRICE = "inverter.manager.force.discharge.minimal.price";
    private static final String CFG_INVERTER_MANAGER_FORCE_DISCHARGE_MINIMAL_SOC = "inverter.manager.force.discharge.minimal.soc";

    /** Length of a single price interval (15 minutes). */
    private static final long SEGMENT_MILLIS = 15 * 60 * 1000L;

    static Logger log = LoggerFactory.getLogger(InverterManager.class.getName());
    private final SolaxInverterModbusClient client;
    private final ElectricitySpotPriceMonitor priceMonitor;
    private final int hardMaxExportPower;
    private final int batteryCapacityWh;
    private final int dischargePowerW;
    private int minimalSoc = -1;
    private int batteryReserve = -1;
    private boolean turnOffOnNegativePrice;
    private boolean exportPriorityTimerEnabled;
    private boolean forceDischargeEnabled;
    private double forceDischargeMinimalPrice = -1;
    private int forceDischargeMinimalSoc = -1;
    private final VppBatteryControl vppBattery;
    private String exportSettingsProblem;
    private final IntervalScheduler highTariffScheduler = new IntervalScheduler(
            () -> applyMinBatterySoc(true),
            () -> applyMinBatterySoc(false)
    );
    private final IntervalScheduler exportPriorityScheduler = new IntervalScheduler(
            () -> applyBatteryMode(true),
            () -> applyBatteryMode(false)
    );

    public InverterManager(SolaxInverterModbusClient client, Options options, ElectricitySpotPriceMonitor priceMonitor,
                           int hardMaxExportPower, int batteryCapacityWh, int dischargePowerW) {
        this.client = client;
        this.priceMonitor = priceMonitor;
        this.hardMaxExportPower = hardMaxExportPower;
        this.vppBattery = new VppBatteryControl(client, isExportAllowed());
        this.batteryCapacityWh = batteryCapacityWh;
        this.dischargePowerW = dischargePowerW;

        setMinimalSoc(options.getInt(CFG_INVERTER_MANAGER_MINIMAL_SOC));
        setBatteryReserve(options.getInt(CFG_INVERTER_MANAGER_HIGH_TARIFF_BATTERY_RESERVE));
        setHighTariffRanges(options.get(CFG_INVERTER_MANAGER_HIGH_TARIFF_TIMES));
        setTurnOffOnNegativePrice(options.getBoolean(CFG_INVERTER_MANAGER_TURNOFF_ON_NEGATIVE_PRICE));
        setExportPriorityTimerEnabled(options.getBoolean(CFG_INVERTER_MANAGER_EXPORT_PRIORITY_TIMER_ENABLED));
        setExportPriorityRanges(options.get(CFG_INVERTER_MANAGER_EXPORT_PRIORITY_TIMES));
        setForceDischargeEnabled(options.getBoolean(CFG_INVERTER_MANAGER_FORCE_DISCHARGE));
        setForceDischargeMinimalPrice(options.getDouble(CFG_INVERTER_MANAGER_FORCE_DISCHARGE_MINIMAL_PRICE));
        setForceDischargeMinimalSoc(options.getInt(CFG_INVERTER_MANAGER_FORCE_DISCHARGE_MINIMAL_SOC));
        applyConfiguration();

        Scheduler.getInstance().scheduleTask("0,15,30,45 * * * *", this::doPowerManagement);
        Scheduler.getInstance().scheduleTask("* * * * *", this::manageForceDischarge);

        options.addListener(new Options.OptionChangeListener() {
            @Override
            public void optionChanged(String key, String value) {
                if (CFG_INVERTER_MANAGER_MINIMAL_SOC.equals(key)) {
                    setMinimalSoc(Integer.parseInt(value));
                } else if (CFG_INVERTER_MANAGER_HIGH_TARIFF_BATTERY_RESERVE.equals(key)) {
                    setBatteryReserve(Integer.parseInt(value));
                } else if (CFG_INVERTER_MANAGER_HIGH_TARIFF_TIMES.equals(key)) {
                    setHighTariffRanges(value);
                } else if (CFG_INVERTER_MANAGER_TURNOFF_ON_NEGATIVE_PRICE.equals(key)) {
                    setTurnOffOnNegativePrice(Boolean.parseBoolean(value));
                } else if (CFG_INVERTER_MANAGER_EXPORT_PRIORITY_TIMER_ENABLED.equals(key)) {
                    setExportPriorityTimerEnabled(Boolean.parseBoolean(value));
                } else if (CFG_INVERTER_MANAGER_EXPORT_PRIORITY_TIMES.equals(key)) {
                    setExportPriorityRanges(value);
                } else if (CFG_INVERTER_MANAGER_FORCE_DISCHARGE.equals(key)) {
                    setForceDischargeEnabled(Boolean.parseBoolean(value));
                } else if (CFG_INVERTER_MANAGER_FORCE_DISCHARGE_MINIMAL_PRICE.equals(key)) {
                    setForceDischargeMinimalPrice(Double.parseDouble(value));
                } else if (CFG_INVERTER_MANAGER_FORCE_DISCHARGE_MINIMAL_SOC.equals(key)) {
                    setForceDischargeMinimalSoc(Integer.parseInt(value));
                }
            }

            @Override
            public void optionsSaved(Set<String> keys) {
                if (keys.contains(CFG_INVERTER_MANAGER_MINIMAL_SOC)
                        || keys.contains(CFG_INVERTER_MANAGER_HIGH_TARIFF_BATTERY_RESERVE)
                        || keys.contains(CFG_INVERTER_MANAGER_HIGH_TARIFF_TIMES)
                        || keys.contains(CFG_INVERTER_MANAGER_EXPORT_PRIORITY_TIMER_ENABLED)
                        || keys.contains(CFG_INVERTER_MANAGER_EXPORT_PRIORITY_TIMES)) {
                    applyConfiguration();
                }
            }
        });
    }

    public boolean isHighTariff() {
        return highTariffScheduler.isInInterval();
    }

    public boolean isExportPriority() {
        return exportPriorityScheduler.isInInterval();
    }

    /**
     * Whether this deployment may feed the grid at all. When it may not, nothing here may start an export:
     * no feed-in priority, no battery discharge to the grid, no Pgrid bias towards the grid.
     */
    public boolean isExportAllowed() {
        return hardMaxExportPower > 0;
    }

    public boolean isForceDischargeActive() {
        return vppBattery.getOwner() == VppBatteryControl.Owner.FORCE_DISCHARGE;
    }

    /**
     * A feature that wants the battery and cannot get it.
     */
    public record BatteryRefusal(String owner, String reason) {
    }

    /**
     * The feature holding the battery through a VPP mode; NONE while the stored work mode applies.
     */
    public String getBatteryOwner() {
        return vppBattery.getOwner().name();
    }

    /**
     * @return null when no feature is refused
     */
    public BatteryRefusal getBatteryRefusal() {
        return vppBattery.getBatteryRefusal();
    }

    public boolean isTurnOffOnNegativePrice() {
        return turnOffOnNegativePrice;
    }

    /**
     * Minimal SoC the manager wants on the inverter right now: the configured minimum, plus the battery reserve
     * outside high tariff hours.
     */
    public int getDesiredMinimalSoc() {
        return isHighTariff() ? minimalSoc : minimalSoc + batteryReserve;
    }

    public int getMinimalSoc() {
        return minimalSoc;
    }

    /**
     * Sets minimal battery charge level in percent.
     */
    public void setMinimalSoc(int minimalSoc) {
        Validate.inclusiveBetween(10, 100, minimalSoc);
        this.minimalSoc = minimalSoc;
    }

    public int getBatteryReserve() {
        return batteryReserve;
    }

    /**
     * Sets battery reserve for high tariff time. Battery SOC is set to setMinimalSoc+batteryReserve in low tariff
     * and to setMinimalSoc in high tariff.
     */
    public void setBatteryReserve(int batteryReserve) {
        log.debug("Setting battery reserve to {}", batteryReserve);
        Validate.inclusiveBetween(0, 90, batteryReserve);
        this.batteryReserve = batteryReserve;
    }

    public void setHighTariffRanges(String intervals) {
        log.debug("setHighTariffRanges({})", intervals);
        highTariffScheduler.setIntervals(intervals);
    }

    public void setTurnOffOnNegativePrice(boolean turnOffOnNegativePrice) {
        log.debug("setTurnOffOnNegativePrice({})", turnOffOnNegativePrice);
        this.turnOffOnNegativePrice = turnOffOnNegativePrice;
    }

    public void setExportPriorityTimerEnabled(boolean enabled) {
        log.debug("setExportPriorityTimerEnabled({})", enabled);
        if (enabled && !isExportAllowed()) {
            log.warn("Export priority timer enabled but export is not allowed in this deployment; it will never activate");
        }
        this.exportPriorityTimerEnabled = enabled;
    }

    public void setExportPriorityRanges(String intervals) {
        log.debug("setExportPriorityRanges({})", intervals);
        exportPriorityScheduler.setIntervals(intervals);
    }

    public void setForceDischargeEnabled(boolean enabled) {
        log.debug("setForceDischargeEnabled({})", enabled);
        if (enabled && !isForceDischargeAllowed()) {
            log.warn("Force discharge enabled but not allowed in this deployment (hardMaxExportPower={}, batteryCapacityWh={}, dischargePowerW={}); it will never activate",
                    hardMaxExportPower, batteryCapacityWh, dischargePowerW);
        }
        this.forceDischargeEnabled = enabled;
    }

    /**
     * Force discharge feeds the battery into the grid, so it is only allowed when grid export is permitted
     * ({@code hardMaxExportPower > 0}) and the battery parameters needed for the discharge-duration estimate
     * are configured ({@code batteryCapacityWh > 0} and {@code dischargePowerW > 0}).
     */
    private boolean isForceDischargeAllowed() {
        return isExportAllowed() && batteryCapacityWh > 0 && dischargePowerW > 0;
    }

    public void setForceDischargeMinimalPrice(double price) {
        log.debug("setForceDischargeMinimalPrice({})", price);
        this.forceDischargeMinimalPrice = price;
    }

    public void setForceDischargeMinimalSoc(int soc) {
        log.debug("setForceDischargeMinimalSoc({})", soc);
        Validate.inclusiveBetween(10, 100, soc);
        this.forceDischargeMinimalSoc = soc;
    }

    public void applyConfiguration() {
        highTariffScheduler.applyCallback();
        exportPriorityScheduler.applyCallback();
    }

    void doPowerManagement() {
        log.debug("doPowerManagement");
        InverterState state = client.getState();
        ElectricitySpotPriceMonitor.IntervalPrice currentPrice = priceMonitor.getPriceAt(System.currentTimeMillis() + 5000);

        if (state == null || currentPrice == null) {
            log.warn("Failed to get inverter state or price");
            return;
        }

        log.debug("buy price: {}, inverterMode: {}", currentPrice.price(), state.getMode());
        // Turn inverter off on negative import price
        if (turnOffOnNegativePrice) {
            if (currentPrice.price() < 0) {
                if (state.getMode().isRunning()) {
                    client.setInverterOn(false);
                }
            } else {
                if (state.getMode() == InverterState.Mode.Waiting) {
                    log.info("turning on inverter");
                }
                if (!state.getMode().isRunning()) {
                    client.setInverterOn(true);
                }
            }
        }

        if (!isExportAllowed()) {
            reportExportSettings(state);
        } else {
            // turn off export on negative export price
            double netSellPrice = netSellPrice(currentPrice);
            int maxExport = (netSellPrice > 0) ? hardMaxExportPower : 0;
            log.debug("maxExport: {} because (price - distributionFee - sellFee) = {} and allowedExport is {}", maxExport, netSellPrice, hardMaxExportPower);
            int currentMaxExport = state.getExportControlUserLimit();
            if (currentMaxExport != maxExport) {
                log.debug("setExportControlUserLimit: {} -> {}", currentMaxExport, maxExport);
                client.setExportControlUserLimit(maxExport);
            } else {
                log.trace("setExportControlUserLimit: already set to {}", maxExport);
            }
        }

        // Re-assert the export-priority battery mode every cycle. A write at the window boundary (e.g. 5:00)
        // can be ignored by the inverter while it is still powering up (right after setInverterOn at the start
        // of the export window), leaving it in SelfUse. This corrects it on the next run once the inverter is up.
        // applyBatteryMode() is a no-op while a VPP mode holds the battery or the export-priority timer is disabled.
        applyBatteryMode(exportPriorityScheduler.isInInterval());
    }

    /**
     * Logs an inverter setting that feeds the grid although export is not allowed. Nothing is written: such a
     * setting was made on purpose at the inverter. Logged once, until it changes.
     */
    private void reportExportSettings(InverterState state) {
        String problem = getExportSettingsProblem(state.getExportControlUserLimit(), state.getPgridBias());
        if (problem != null && !problem.equals(exportSettingsProblem)) {
            log.error("Export is not allowed in this deployment, but {}", problem);
        }
        exportSettingsProblem = problem;
    }

    /**
     * Inverter settings that feed the grid; null when there are none.
     */
    static String getExportSettingsProblem(int exportLimitW, InverterState.PgridBias pgridBias) {
        List<String> problems = new ArrayList<>();
        if (exportLimitW != 0) {
            problems.add("the inverter's export limit is " + exportLimitW + " W");
        }
        if (pgridBias == InverterState.PgridBias.Grid) {
            problems.add("Pgrid bias is Grid");
        }
        return problems.isEmpty() ? null : String.join(" and ", problems);
    }

    /**
     * Stored work mode the export priority timer wants; null when the timer must not touch it.
     */
    static InverterState.BatteryMode getTimerBatteryMode(boolean exportAllowed, boolean timerEnabled,
                                                         boolean inExportPriority) {
        if (!exportAllowed || !timerEnabled) {
            return null;
        }
        return inExportPriority ? InverterState.BatteryMode.FeedInPriority : InverterState.BatteryMode.SelfUse;
    }

    void applyMinBatterySoc(boolean enteringHighTariff) {
        try {
            Validate.inclusiveBetween(10, 100, minimalSoc);
            Validate.inclusiveBetween(0, 90, batteryReserve);
            Validate.inclusiveBetween(10, 100, minimalSoc + batteryReserve);
            int minSoc = minimalSoc;
            if (enteringHighTariff) {
                log.debug("setMinBatterySoc: Entering high tariff, minSOC={}}", minSoc);
            } else {
                minSoc += batteryReserve;
                log.debug("setMinBatterySoc: Entering low tariff, minSOC={} ({}+{})}", minSoc, minimalSoc, batteryReserve);
            }

            int origValue = client.getState().getSelfUseMinimalSoc();
            if (minSoc != origValue) {
                client.setSelfUseMinimalSoc(minSoc);
                int storedValue = client.getState().getSelfUseMinimalSoc();
                if (storedValue != minSoc) {
                    log.error("Failed to set MinBatterySoc to {}, stored value is {}", minSoc, storedValue);
                }
            } else {
                log.debug("minBatterySoc already set to {}, no change needed", minSoc);
            }
        } catch (Exception e) {
            log.error("Failed to set MinBatterySoc", e);
        }
    }

    void applyBatteryMode(boolean enteringExportPriority) {
        try {
            if (vppBattery.getOwner() != VppBatteryControl.Owner.NONE) {
                log.debug("applyBatteryMode skipped, {} holds the battery", vppBattery.getOwner());
                return;
            }
            InverterState.BatteryMode desired = getTimerBatteryMode(isExportAllowed(), exportPriorityTimerEnabled,
                    enteringExportPriority);
            if (desired == null) {
                log.debug("applyBatteryMode skipped, export priority timer disabled or export not allowed");
                return;
            }
            InverterState.BatteryMode current = client.getState().getBatteryMode();
            if (current != desired) {
                log.debug("setBatteryMode: {} -> {}", current, desired);
                client.setBatteryMode(desired);
                InverterState.BatteryMode storedValue = client.getState().getBatteryMode();
                if (storedValue != desired) {
                    // Typically happens when the inverter is still powering up; the next doPowerManagement
                    // cycle re-asserts the mode once it is up.
                    log.error("Failed to set BatteryMode to {}, stored value is {}", desired, storedValue);
                }
            } else {
                log.debug("batteryMode already set to {}, no change needed", desired);
            }
        } catch (Exception e) {
            log.error("Failed to set BatteryMode", e);
        }
    }

    /**
     * Every minute decides whether the battery should be force-discharged to the grid during the current price
     * segment. The discharge runs as a VPP mode that is re-issued here while it should go on; once it should
     * stop (segment no longer among the highest-price ones, SoC floor reached, feature disabled, or no prices)
     * the mode is ended and control returns to the {@link #exportPriorityScheduler}.
     */
    void manageForceDischarge() {
        try {
            InverterState state = client.getState();
            if (state == null) {
                log.warn("manageForceDischarge: failed to get inverter state");
                return;
            }
            // Force discharge feeds energy to the grid, so it must never run when disabled or not allowed
            // in this deployment (no export, or missing battery parameters).
            boolean enabled = forceDischargeEnabled && isForceDischargeAllowed();
            if (enabled && leaveStoredForceDischarge(state)) {
                return;
            }
            vppBattery.clearLeftoverOnce(state);

            int batteryW = 0;
            if (enabled) {
                ElectricitySpotPriceMonitor.Prices dayPrices = priceMonitor.getDayPrices();
                if (dayPrices == null) {
                    log.warn("manageForceDischarge: failed to get day prices");
                } else if (shouldDischargeNow(dayPrices.prices(), System.currentTimeMillis(),
                        state.getBatterySoc(), forceDischargeMinimalSoc, forceDischargeMinimalPrice,
                        batteryCapacityWh, dischargePowerW)) {
                    batteryW = getForceDischargeBatteryW(dischargePowerW, hardMaxExportPower,
                            state.getPv1Power() + state.getPv2Power());
                }
                log.debug("manageForceDischarge: soc={}, minimalSoc={}, minimalPrice={} -> battery {} W",
                        state.getBatterySoc(), forceDischargeMinimalSoc, forceDischargeMinimalPrice, batteryW);
            }
            boolean wasActive = isForceDischargeActive();
            if (batteryW > 0) {
                vppBattery.hold(VppBatteryControl.Owner.FORCE_DISCHARGE, state, VppBatteryControl.PV_UNLIMITED_W,
                        batteryW);
            } else {
                vppBattery.release(VppBatteryControl.Owner.FORCE_DISCHARGE);
            }
            if (wasActive && !isForceDischargeActive()) {
                // applyBatteryMode() was suppressed while the battery was held
                exportPriorityScheduler.applyCallback();
            }
        } catch (Exception e) {
            log.error("manageForceDischarge failed", e);
        }
    }

    /**
     * Switches a stored Manual / ForceDischarge back to SelfUse. Force discharge used to run that way, so the
     * state can be left by a previous version of the application; a VPP mode cannot start from it.
     *
     * @return whether the stored work mode was changed
     */
    private boolean leaveStoredForceDischarge(InverterState state) {
        if (state.getBatteryMode() != InverterState.BatteryMode.Manual
                || state.getManualMode() != InverterState.ManualMode.ForceDischarge) {
            return false;
        }
        log.info("Force discharge: leaving the stored Manual / ForceDischarge mode");
        client.setBatteryMode(InverterState.BatteryMode.SelfUse);
        // overrides SelfUse with FeedInPriority when an export-priority window is active
        exportPriorityScheduler.applyCallback();
        return true;
    }

    /**
     * Battery discharge power that keeps the export within the allowed one. PV counts in full and the house
     * load is ignored, which errs on the low side. Not positive when PV alone fills the allowance.
     */
    static int getForceDischargeBatteryW(int dischargePowerW, int hardMaxExportPower, int pvPowerW) {
        return Math.min(dischargePowerW, hardMaxExportPower - pvPowerW);
    }

    /**
     * Net revenue per kWh actually earned by exporting this segment to the grid:
     * buy price minus the distribution fee (a buy-side cost, not earned on export) and the sell fee.
     */
    public static double netSellPrice(IntervalPrice p) {
        return p.price() - p.distributionFee() - p.sellFee();
    }

    /**
     * Pure decision function (no I/O) deciding whether the price segment containing {@code now} is one
     * of the highest-price segments of the current day that are needed to discharge the battery from
     * {@code soc} down to {@code minimalSoc}.
     * <p>
     * It estimates how many 15-minute segments are needed to drain the usable energy at the battery's
     * maximum discharge power, then picks that many of the remaining segments of today with the highest
     * {@link #netSellPrice(IntervalPrice) net sell price} that is at least {@code minimalPrice}, and
     * returns whether the current segment is among them.
     */
    static boolean shouldDischargeNow(IntervalPrice[] dayPrices, long now, int soc, int minimalSoc,
                                      double minimalPrice, int batteryCapacityWh, int dischargePowerW) {
        if (dayPrices == null || soc <= minimalSoc) {
            return false;
        }
        double usableWh = (soc - minimalSoc) / 100.0 * batteryCapacityWh;
        double energyPerSegmentWh = dischargePowerW * (SEGMENT_MILLIS / 3_600_000.0);
        int segmentsNeeded = (int)Math.ceil(usableWh / energyPerSegmentWh);
        if (segmentsNeeded <= 0) {
            return false;
        }

        LocalDate today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate();
        IntervalPrice current = null;
        List<IntervalPrice> candidates = new ArrayList<>();
        for (IntervalPrice p : dayPrices) {
            // skip segments already fully in the past
            if (p.time() + SEGMENT_MILLIS <= now) {
                continue;
            }
            // horizon: current calendar day only
            LocalDate day = Instant.ofEpochMilli(p.time()).atZone(ZoneId.systemDefault()).toLocalDate();
            if (!day.equals(today)) {
                continue;
            }
            if (p.time() <= now && now < p.time() + SEGMENT_MILLIS) {
                current = p;
            }
            if (netSellPrice(p) >= minimalPrice) {
                candidates.add(p);
            }
        }

        if (current == null || netSellPrice(current) < minimalPrice) {
            return false;
        }

        // Rank by net sell price desc; the current segment must be within the top `segmentsNeeded` of them.
        candidates.sort(Comparator.comparingDouble(InverterManager::netSellPrice).reversed());
        int limit = Math.min(segmentsNeeded, candidates.size());
        for (int i = 0; i < limit; i++) {
            if (candidates.get(i).time() == current.time()) {
                return true;
            }
        }
        return false;
    }
}
