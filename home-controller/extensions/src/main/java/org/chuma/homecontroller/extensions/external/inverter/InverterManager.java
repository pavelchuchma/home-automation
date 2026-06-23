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
    /** True while we have switched the inverter into Manual/ForceDischarge ourselves. */
    private volatile boolean forceDischargeActive = false;
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
        return hardMaxExportPower > 0 && batteryCapacityWh > 0 && dischargePowerW > 0;
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
                if (state.getMode() == InverterState.Mode.Normal) {
                    client.setInverterOn(false);
                }
            } else {
                if (state.getMode() == InverterState.Mode.Waiting) {
                    log.info("turning on inverter");
                }
                if (state.getMode() != InverterState.Mode.Normal) {
                    client.setInverterOn(true);
                }
            }
        }

        if (hardMaxExportPower > 0) {
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
            if (forceDischargeActive) {
                log.debug("applyBatteryMode skipped, force discharge active");
                return;
            }
            if (!exportPriorityTimerEnabled) {
                log.debug("applyBatteryMode skipped, export priority timer disabled");
                return;
            }
            InverterState.BatteryMode desired = enteringExportPriority
                    ? InverterState.BatteryMode.FeedInPriority
                    : InverterState.BatteryMode.SelfUse;
            InverterState.BatteryMode current = client.getState().getBatteryMode();
            if (current != desired) {
                log.debug("setBatteryMode: {} -> {}", current, desired);
                client.setBatteryMode(desired);
            } else {
                log.debug("batteryMode already set to {}, no change needed", desired);
            }
        } catch (Exception e) {
            log.error("Failed to set BatteryMode", e);
        }
    }

    /**
     * Periodically (every minute) decides whether the battery should be force-discharged to the grid
     * during the current price segment. While active the inverter is switched to Manual/ForceDischarge;
     * once it should stop (segment no longer among the highest-price ones, SoC floor reached, or feature
     * disabled) control is returned to the {@link #exportPriorityScheduler}.
     */
    void manageForceDischarge() {
        try {
            // Force discharge feeds energy to the grid, so it must never run when disabled or not allowed
            // in this deployment (no export, or missing battery parameters).
            if (!forceDischargeEnabled || !isForceDischargeAllowed()) {
                stopForceDischarge();
                return;
            }
            InverterState state = client.getState();
            ElectricitySpotPriceMonitor.Prices dayPrices = priceMonitor.getDayPrices();
            if (state == null || dayPrices == null) {
                log.warn("manageForceDischarge: failed to get inverter state or day prices");
                return;
            }
            // Adopt a Manual/ForceDischarge state left by a previous run (e.g. after a restart during discharge)
            // so stopForceDischarge() below can tear it down correctly instead of leaving the battery
            // discharging to the grid unmanaged (stopForceDischarge() is a no-op while the flag is false).
            if (state.getBatteryMode() == InverterState.BatteryMode.Manual
                    && state.getManualMode() == InverterState.ManualMode.ForceDischarge) {
                forceDischargeActive = true;
            }
            boolean discharge = shouldDischargeNow(dayPrices.prices(), System.currentTimeMillis(),
                    state.getBatterySoc(), forceDischargeMinimalSoc, forceDischargeMinimalPrice,
                    batteryCapacityWh, dischargePowerW);
            log.debug("manageForceDischarge: soc={}, minimalSoc={}, minimalPrice={} -> discharge={}",
                    state.getBatterySoc(), forceDischargeMinimalSoc, forceDischargeMinimalPrice, discharge);
            if (discharge) {
                startForceDischarge(state);
            } else {
                stopForceDischarge();
            }
        } catch (Exception e) {
            log.error("manageForceDischarge failed", e);
        }
    }

    private void startForceDischarge(InverterState state) {
        if (state.getBatteryMode() != InverterState.BatteryMode.Manual) {
            log.info("Force discharge: setting BatteryMode to Manual");
            client.setBatteryMode(InverterState.BatteryMode.Manual);
        }
        if (state.getManualMode() != InverterState.ManualMode.ForceDischarge) {
            log.info("Force discharge: setting ManualMode to ForceDischarge");
            client.setManualMode(InverterState.ManualMode.ForceDischarge);
        }
        forceDischargeActive = true;
    }

    private void stopForceDischarge() {
        if (!forceDischargeActive) {
            return;
        }
        log.info("Force discharge: stopping, returning control to export priority scheduler");
        // Clear the flag first so applyBatteryMode() is no longer suppressed.
        forceDischargeActive = false;
        // Leave Manual mode back to the normal self-use baseline; applyCallback() below overrides it
        // with FeedInPriority when an export-priority window is currently active (it is a no-op when the
        // export-priority timer is disabled, hence the explicit SelfUse here).
        client.setBatteryMode(InverterState.BatteryMode.SelfUse);
        exportPriorityScheduler.applyCallback();
    }

    /**
     * Net revenue per kWh actually earned by exporting this segment to the grid:
     * buy price minus the distribution fee (a buy-side cost, not earned on export) and the sell fee.
     */
    static double netSellPrice(IntervalPrice p) {
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
