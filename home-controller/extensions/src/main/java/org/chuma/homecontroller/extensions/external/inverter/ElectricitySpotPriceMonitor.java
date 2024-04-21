package org.chuma.homecontroller.extensions.external.inverter;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.extensions.external.inverter.impl.HttpJsonClient;

public class ElectricitySpotPriceMonitor {
    static Logger log = LoggerFactory.getLogger(InverterManager.class.getName());

    public static final double VAT_RATE = 1.21;
    public static final int MAX_MISSING_ENTRY_CACHE_DURATION_MS = 5 * 60_000; // 5 min
    private final Cache<double[]> cache = new Cache<>();
    private double lastExchangeRateValue = 0;
    private long lastExchangeRateTimestamp = 0;


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

    private static class Cache<E> {
        class Entry {
            final E data;

            final long timestamp;

            public Entry(E data) {
                this.data = data;
                timestamp = System.currentTimeMillis();
            }
        }

        Map<String, Entry> map = new HashMap<>();

        String buildKey(Calendar date) {
            return String.format("%d-%d-%d", date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH));
        }

        Entry get(Calendar date) {
            return map.get(buildKey(date));
        }

        void set(Calendar date, E data) {
            map.put(buildKey(date), new Entry(data));
        }

    }

    synchronized double getCurrentEurCzkExchangeRate() {
        if (System.currentTimeMillis() - lastExchangeRateTimestamp < 3_600_000) {
            return lastExchangeRateValue;
        }
        String url = "https://data.kurzy.cz/json/meny/b[6].json";
        log.debug("getting EUR/CZK exchange rate from: {}", url);
        try {
            HttpJsonClient client = new HttpJsonClient(url, 10);
            JsonObject response = client.doGet("");
            BigDecimal value = (BigDecimal)((JsonObject)((JsonObject)response.get("kurzy")).get("EUR")).get("dev_stred");
            lastExchangeRateValue = value.doubleValue();
            lastExchangeRateTimestamp = System.currentTimeMillis();
            log.debug("  got {}", lastExchangeRateValue);
            return lastExchangeRateValue;
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to get EUR/CZK rate from kurzy.cz", e);
        }
    }

    public synchronized Prices getDayPrices() {
        try {
            double exchangeRate = getCurrentEurCzkExchangeRate();
            double distributionPrice = (450.43 + 212.82 + 495.00);

            Calendar now = new GregorianCalendar();

            double[] todayPrices = getOneDayPricesImpl(0);
            double[] tomorrowPrices = getOneDayPricesImpl(1);

            double[] result;
            int currentEntry;
            if (tomorrowPrices != null) {
                result = ArrayUtils.addAll(todayPrices, tomorrowPrices);
                currentEntry = now.get(Calendar.HOUR_OF_DAY);
            } else {
                double[] yesterdayPrices = getOneDayPricesImpl(-1);
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
        date.add(Calendar.DAY_OF_MONTH, daysFromToday);

        Cache<double[]>.Entry entry = cache.get(date);
        if (entry != null) {
            if (entry.data != null || System.currentTimeMillis() - entry.timestamp > MAX_MISSING_ENTRY_CACHE_DURATION_MS) {
                return entry.data;
            }
        }

        double[] data = getDayPricesImpl(date);
        cache.set(date, data);
        return data;
    }

    private static double[] getDayPricesImpl(Calendar date) {
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
            JsonArray points = (JsonArray)((JsonObject)dataLines.get(1)).get("point");
            if (points.size() != 24) {
                throw new IllegalStateException("Unexpected response content");
            }
            double[] result = new double[24];
            for (int i = 0; i < 24; i++) {
                BigDecimal d = (BigDecimal)((JsonObject)points.get(i)).get("y");
                result[i] = d.doubleValue();
            }
            log.debug("  OK response");
            return result;
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to get electricity prices from www.ote-cr.cz", e);
        }
    }
}
