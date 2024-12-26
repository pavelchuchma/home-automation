package org.chuma.homecontroller.extensions.external.inverter;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.GregorianCalendar;

import com.github.cliftonlabs.json_simple.JsonObject;

import org.chuma.homecontroller.extensions.external.inverter.impl.HttpJsonClient;

class ExchangeRateMonitor {
    DailyValueCache<Double> cache = new DailyValueCache<>(5 * 60 * 60, 5 * 60) {
        @Override
        public Double getEntryImpl(Calendar date) {
            String url = "https://data.kurzy.cz/json/meny/b[6].json";
            ElectricitySpotPriceMonitor.log.debug("getting EUR/CZK exchange rate from: {}", url);
            HttpJsonClient client = new HttpJsonClient(url, 10);
            JsonObject response = client.doGet("");
            BigDecimal value = (BigDecimal)((JsonObject)((JsonObject)response.get("kurzy")).get("EUR")).get("dev_stred");
            ElectricitySpotPriceMonitor.log.debug("  got {}", value.doubleValue());
            return value.doubleValue();
        }
    };
    // any value, cache will have only one entry. Big differences between days are not expected.
    private static final GregorianCalendar cacheKey = new GregorianCalendar();

    synchronized Double getCurrentEurCzkExchangeRate() {
        return cache.get(cacheKey);
    }
}
