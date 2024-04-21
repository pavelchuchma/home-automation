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

        jw.addAttribute("dist", dayPrices.distributionPrice);
        jw.addAttribute("currentEntry", dayPrices.currentEntry);
        try (JsonWriter arr = jw.startArrayAttribute("data")) {
            for (double value : dayPrices.prices) {
                jw.addArrayValue(value);
            }
        }
    }
}
