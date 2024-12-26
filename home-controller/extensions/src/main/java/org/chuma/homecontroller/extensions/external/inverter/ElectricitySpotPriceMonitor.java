package org.chuma.homecontroller.extensions.external.inverter;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.GregorianCalendar;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.extensions.external.inverter.impl.HttpJsonClient;

public class ElectricitySpotPriceMonitor {
    static Logger log = LoggerFactory.getLogger(ElectricitySpotPriceMonitor.class.getName());

    private static final double VAT_RATE = 1.21;
    final DailyValueCache<double[]> cache = new DailyValueCache<>(3 * 24 * 60 * 60, 10 * 60) {
        @Override
        public double[] getEntryImpl(Calendar date) {
            try {
                String url = String.format("https://www.ote-cr.cz/cs/kratkodobe-trhy/elektrina/denni-trh/@@chart-data?report_date=%d-%d-%d",
                        date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH));

                log.debug("getting day prices: {}", url);
                HttpJsonClient client = new HttpJsonClient(url, 10);
                JsonObject response = client.doGet("");
                JsonArray dataLines = (JsonArray)((JsonObject)response.get("data")).get("dataLine");
                if (dataLines.isEmpty()) {
                    log.debug("  empty response");
                    return null;
                }
                JsonArray points = normalizeDayPricesArray((JsonArray)((JsonObject)dataLines.get(1)).get("point"));
                double[] result = new double[24];
                for (int i = 0; i < 24; i++) {
                    BigDecimal d = (BigDecimal)((JsonObject)points.get(i)).get("y");
                    result[i] = d.doubleValue();
                }
                log.debug("  OK response");
                return result;
            } catch (RuntimeException e) {
                log.error("Failed to get electricity prices from www.ote-cr.cz", e);
                return null;
            }
        }
    };
    private final ExchangeRateMonitor exchangeRateMonitor = new ExchangeRateMonitor();

    public static class Prices {
        public final double[] prices;
        public final double distributionPrice;
        public final int currentEntry;

        public Prices(double[] prices, double distributionPrice, int currentEntry) {
            this.prices = prices;
            this.distributionPrice = distributionPrice;
            this.currentEntry = currentEntry;
        }
    }

    public synchronized Prices getDayPrices() {
        try {
            Double exchangeRate = exchangeRateMonitor.getCurrentEurCzkExchangeRate();
            // Cena za distribuované množství energie (v Kč/MWh): 450.53
            // Systémové služby: 212.82
            // Cena na podporu elektřiny z podporovaných zdrojů elektřiny (POZE): 495
            // Cena za služby obchodu Kč/MWh: 450
            double distributionPrice = (450.43 + 212.82 + 495.00 + 450);

            double[] todayPrices = getOneDayPricesImpl(0);
            double[] tomorrowPrices = getOneDayPricesImpl(1);
            double[] yesterdayPrices = (tomorrowPrices == null) ? getOneDayPricesImpl(-1) : null;

            if (exchangeRate == null || todayPrices == null || (yesterdayPrices == null && tomorrowPrices == null)) {
                // data not available (yet)
                return null;
            }

            Calendar now = new GregorianCalendar();
            double[] result;
            int currentEntry;
            if (tomorrowPrices != null) {
                result = ArrayUtils.addAll(todayPrices, tomorrowPrices);
                currentEntry = now.get(Calendar.HOUR_OF_DAY);
            } else {
                result = ArrayUtils.addAll(yesterdayPrices, todayPrices);
                currentEntry = now.get(Calendar.HOUR_OF_DAY) + 24;
            }

            for (int i = 0; i < 48; i++) {
                result[i] = (result[i] * exchangeRate + distributionPrice) * VAT_RATE / 1000;
            }

            return new Prices(result, distributionPrice * VAT_RATE / 1000, currentEntry);
        } catch (RuntimeException e) {
            log.error("Failed to get electricity day prices", e);
            return null;
        }
    }

    private double[] getOneDayPricesImpl(int daysFromToday) {
        Calendar date = new GregorianCalendar();
        if (daysFromToday > 0 && date.get(Calendar.HOUR_OF_DAY) < 12) {
            // tomorrow's prices are published ~14:00, no reason to try it much earlier
            return null;
        }
        date.add(Calendar.DAY_OF_MONTH, daysFromToday);
        return cache.get(date);
    }

    private static JsonArray normalizeDayPricesArray(JsonArray values) {
        if (values.size() == 25) {
            // summer -> winter time -> remove 4th hour
            values.remove(2);
        } else if (values.size() == 23) {
            // winter -> summer -> duplicate 3rd hour
            values.add(2, values.get(2));
        }
        if (values.size() != 24) {
            throw new IllegalStateException("Unexpected response content");
        }
        return values;
    }
}
