package org.chuma.homecontroller.app.servlet.rest;

import javax.servlet.http.HttpServletRequest;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.extensions.external.inverter.ElectricitySpotPriceMonitor;

public class ElectricitySpotPriceHandler extends AbstractRestHandler<ElectricitySpotPriceMonitor> {
    public ElectricitySpotPriceHandler(Iterable<ElectricitySpotPriceMonitor> monitors) {
        super("eprice", "eprice", monitors, (o) -> "eprice");
    }

    @Override
    void writeJsonItemValues(JsonWriter jw, ElectricitySpotPriceMonitor monitor, HttpServletRequest request) {
        ElectricitySpotPriceMonitor.Prices dayPrices = monitor.getDayPrices();
        if (dayPrices == null) {
            return;
        }

        jw.addAttribute("distFee", dayPrices.distributionFee());
        jw.addAttribute("sellFee", dayPrices.sellFee());
        try (JsonWriter aw = jw.startArrayAttribute("values")) {
            for (ElectricitySpotPriceMonitor.IntervalPrice value : dayPrices.prices()) {
                try (JsonWriter objectWriter = aw.startObject()) {
                    objectWriter.addAttribute("time", value.time());
                    objectWriter.addAttribute("price", value.price());
                }
            }
        }
    }
}
