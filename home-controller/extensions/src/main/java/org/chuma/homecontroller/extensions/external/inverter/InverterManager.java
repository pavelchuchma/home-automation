package org.chuma.homecontroller.extensions.external.inverter;

import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.utils.Options;
import org.chuma.homecontroller.extensions.external.inverter.impl.SolaxInverterModbusClient;
import org.chuma.homecontroller.extensions.external.utils.IntervalScheduler;

public class InverterManager {
    private static final String CFG_INVERTER_MANAGER_HIGH_TARIFF_BATTERY_RESERVE = "inverter.manager.high.tariff.battery.reserve";
    private static final String CFG_INVERTER_MANAGER_HIGH_TARIFF_TIMES = "inverter.manager.high.tariff.times";
    private static final String CFG_INVERTER_MANAGER_MINIMAL_SOC = "inverter.manager.minimal.soc";
    private static final String CFG_INVERTER_MANAGER_TURNOFF_ON_NEGATIVE_PRICE = "inverter.manager.turnoff.on.negative.price";

    static Logger log = LoggerFactory.getLogger(InverterManager.class.getName());
    private final SolaxInverterModbusClient client;
    private final ElectricitySpotPriceMonitor priceMonitor;
    private final int hardMaxExportPower;
    private int minimalSoc = -1;
    private int batteryReserve = -1;
    private boolean turnOffOnNegativePrice;
    private final IntervalScheduler intervalScheduler = new IntervalScheduler(
            () -> applyMinBatterySoc(true),
            () -> applyMinBatterySoc(false)
    );

    public InverterManager(SolaxInverterModbusClient client, Options options, ElectricitySpotPriceMonitor priceMonitor, int hardMaxExportPower) {
        this.client = client;
        this.priceMonitor = priceMonitor;
        this.hardMaxExportPower = hardMaxExportPower;

        setMinimalSoc(options.getInt(CFG_INVERTER_MANAGER_MINIMAL_SOC));
        setBatteryReserve(options.getInt(CFG_INVERTER_MANAGER_HIGH_TARIFF_BATTERY_RESERVE));
        setHighTariffRanges(options.get(CFG_INVERTER_MANAGER_HIGH_TARIFF_TIMES));
        setTurnOffOnNegativePrice(options.getBoolean(CFG_INVERTER_MANAGER_TURNOFF_ON_NEGATIVE_PRICE));
        applyConfiguration();

        Scheduler.getInstance().scheduleTask("0,15,30,45 * * * *", this::doPowerManagement);

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
                }
            }

            @Override
            public void optionsSaved(Set<String> keys) {
                if (keys.contains(CFG_INVERTER_MANAGER_MINIMAL_SOC)
                        || keys.contains(CFG_INVERTER_MANAGER_HIGH_TARIFF_BATTERY_RESERVE)
                        || keys.contains(CFG_INVERTER_MANAGER_HIGH_TARIFF_TIMES)) {
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
        intervalScheduler.setIntervals(intervals);
    }

    public void setTurnOffOnNegativePrice(boolean turnOffOnNegativePrice) {
        log.debug("setTurnOffOnNegativePrice({})", turnOffOnNegativePrice);
        this.turnOffOnNegativePrice = turnOffOnNegativePrice;
    }

    public void applyConfiguration() {
        intervalScheduler.applyCallback();
    }

    void doPowerManagement() {
        log.debug("doPowerManagement");
        // TODO: implement power management logic
        InverterState state = client.getState();
        ElectricitySpotPriceMonitor.IntervalPrice currentPrice = priceMonitor.getPriceAt(System.currentTimeMillis() + 5000);

        if (state == null || currentPrice == null) {
            log.warn("Failed to get inverter state or price");
            return;
        }

        log.debug("buy price: {}, inverterMode: {}", currentPrice.price(), state.getMode());
        if (turnOffOnNegativePrice) {
            if (currentPrice.price() < 0) {
                if (state.getMode() == InverterState.Mode.Normal) {
                    client.setInverterOn(false);
                }
            } else {
                if (state.getMode() == InverterState.Mode.Waiting) {
                    log.info("turning on inverter");
                }
                client.setInverterOn(true);
            }
        }

        // turn off export
        if (hardMaxExportPower > 0) {
            double netSellPrice = currentPrice.price() - currentPrice.distributionFee() - currentPrice.sellFee();
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
}
