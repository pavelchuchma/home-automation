package org.chuma.homecontroller.extensions.metrics;

import org.chuma.homecontroller.extensions.external.AbstractStateMonitor;
import org.chuma.homecontroller.extensions.external.inverter.ElectricitySpotPriceMonitor;
import org.chuma.homecontroller.extensions.external.inverter.ElectricitySpotPriceMonitor.IntervalPrice;
import org.chuma.homecontroller.extensions.external.inverter.InverterManager;
import org.chuma.homecontroller.extensions.external.inverter.InverterState;
import org.chuma.homecontroller.extensions.external.inverter.impl.SolaxInverterMonitor;

/**
 * {@code inverter}: the Solax state. {@code inverter-manager}: what the manager decided and the price it saw,
 * so a mode change in the inverter series can be explained.
 */
public class InverterMetrics implements MetricSource {
    private final SolaxInverterMonitor monitor;
    private final InverterManager manager;
    private final ElectricitySpotPriceMonitor priceMonitor;

    public InverterMetrics(SolaxInverterMonitor monitor, InverterManager manager, ElectricitySpotPriceMonitor priceMonitor) {
        this.monitor = monitor;
        this.manager = manager;
        this.priceMonitor = priceMonitor;
    }

    @Override
    public void sample(LineProtocolWriter writer, long nowMs) {
        AbstractStateMonitor.Snapshot<InverterState> snapshot = monitor.peekState();
        if (snapshot != null) {
            InverterState s = snapshot.value;
            writer.line("inverter", nowMs)
                    .field("mode", s.getMode())
                    .field("battery_mode", s.getBatteryMode())
                    .field("manual_mode", s.getManualMode())
                    .field("pgrid_bias", s.getPgridBias())
                    .field("pv1_w", s.getPv1Power())
                    .field("pv2_w", s.getPv2Power())
                    .field("grid_w", s.getGridPower())
                    .field("grid1_w", s.getGrid1Power())
                    .field("grid2_w", s.getGrid2Power())
                    .field("grid3_w", s.getGrid3Power())
                    .field("feed_in_w", s.getFeedInPower())
                    .field("battery_w", s.getBatteryPower())
                    .field("eps_w", s.getEpsPower())
                    .field("soc", s.getBatterySoc())
                    .field("batt_temp_c", s.getBatteryTemp())
                    .field("batt_v", s.getBatteryVoltage())
                    .field("grid1_v", s.getGrid1Voltage())
                    .field("grid2_v", s.getGrid2Voltage())
                    .field("grid3_v", s.getGrid3Voltage())
                    .field("yield_today_kwh", s.getYieldToday())
                    .field("pv_yield_today_kwh", s.getPvYieldToday())
                    .field("feed_in_today_kwh", s.getFeedInEnergyToday())
                    .field("consumed_today_kwh", s.getConsumedEnergyToday())
                    .field("yield_total_kwh", s.getYieldTotal())
                    .field("feed_in_total_kwh", s.getFeedInEnergyTotal())
                    .field("consumed_total_kwh", s.getConsumedEnergyTotal())
                    .field("min_soc", s.getSelfUseMinimalSoc())
                    .field("export_limit_w", s.getExportControlUserLimit())
                    .end();
        }
        if (manager != null) {
            LineProtocolWriter.Line line = writer.line("inverter-manager", nowMs)
                    .field("high_tariff", manager.isHighTariff())
                    .field("export_priority", manager.isExportPriority())
                    .field("force_discharge", manager.isForceDischargeActive())
                    .field("inverter_off_negative_price", manager.isTurnOffOnNegativePrice());
            if (manager.getMinimalSoc() >= 0 && manager.getBatteryReserve() >= 0) {
                line.field("desired_min_soc", manager.getDesiredMinimalSoc());
            }
            IntervalPrice price = priceMonitor.getPriceAt(nowMs);
            if (price != null) {
                line.field("buy_price", price.price()).field("sell_price_net", InverterManager.netSellPrice(price));
            }
            line.end();
        }
    }
}
