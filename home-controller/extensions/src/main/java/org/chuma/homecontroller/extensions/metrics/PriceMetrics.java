package org.chuma.homecontroller.extensions.metrics;

import java.util.HashSet;
import java.util.Set;

import org.chuma.homecontroller.extensions.external.inverter.ElectricitySpotPriceMonitor;
import org.chuma.homecontroller.extensions.external.inverter.ElectricitySpotPriceMonitor.IntervalPrice;
import org.chuma.homecontroller.extensions.external.inverter.InverterManager;

/**
 * {@code price}: one point per 15-minute interval, written as soon as the day's prices are known, with the
 * interval start as the timestamp. Tomorrow's prices therefore appear in Grafana in the afternoon. Points
 * already written are remembered; a restart rewrites them, which InfluxDB treats as an overwrite.
 */
public class PriceMetrics implements MetricSource {
    private static final long FORGET_AFTER_MS = 2 * 24 * 3600_000L;

    private final ElectricitySpotPriceMonitor priceMonitor;
    private final Set<Long> written = new HashSet<>();

    public PriceMetrics(ElectricitySpotPriceMonitor priceMonitor) {
        this.priceMonitor = priceMonitor;
    }

    @Override
    public void sample(LineProtocolWriter writer, long nowMs) {
        ElectricitySpotPriceMonitor.Prices prices = priceMonitor.getDayPrices();
        if (prices == null) {
            return;
        }
        for (IntervalPrice p : prices.prices()) {
            if (written.add(p.time())) {
                writer.line("price", p.time())
                        .field("buy", p.price())
                        .field("spot", p.price() - p.distributionFee())
                        .field("sell_net", InverterManager.netSellPrice(p))
                        .end();
            }
        }
        written.removeIf(t -> t < nowMs - FORGET_AFTER_MS);
    }
}
