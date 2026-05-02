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

    public record RawIntervalPrice(long time, double price) {
    }
    public record IntervalPrice(long time, double price, double distributionFee, double sellFee) {
    }

    final DailyValueCache<RawIntervalPrice[]> cache = new DailyValueCache<>(3 * 24 * 60 * 60, 10 * 60) {
        @Override
        public RawIntervalPrice[] getEntryImpl(Calendar date) {
            try {
                String url = String.format("https://www.ote-cr.cz/pw-data/chart-data/01?report_date=%d-%02d-%02d&time_resolution=PT15M&language=en",
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

                double[] prices = getPriceArray(points);
                // 15-minute intervals
                RawIntervalPrice[] result = new RawIntervalPrice[prices.length];
                GregorianCalendar time = new GregorianCalendar(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
                long millis = time.getTimeInMillis();

                for (int i = 0; i < prices.length; i++) {
                    result[i] = new RawIntervalPrice(millis, prices[i]);
                    millis += 15 * 60_000;
                }
                log.debug("  OK response");
                return result;
            } catch (RuntimeException e) {
                log.error("Failed to get electricity prices from www.ote-cr.cz", e);
                return null;
            }
        }
    };

    private static double[] getPriceArray(JsonArray points) {
        // service returns hour intervals for dates before 2025-10-01
        boolean quarterFormat = points.size() > 25;
        double[] result = new double[(quarterFormat) ? points.size() : points.size() * 4];
        for (int i = 0; i < result.length; i++) {
            BigDecimal d = (BigDecimal)((JsonObject)points.get((quarterFormat) ? i : i / 4)).get("y");
            result[i] = d.doubleValue();
        }
        return result;
    }

    private final ExchangeRateMonitor exchangeRateMonitor = new ExchangeRateMonitor();
    private final double distributionFeeKwInclVat;
    private final double sellFeeKwInclVat;
    private final double vatRate;

    /**
     * @param distributionFeeWithoutVat buying fee in CZK per MWh without VAT
     * @param sellFeeWithoutVat         selling fee in CZK per MWh without VAT
     * @param vatRate                   VAT rate in %
     */
    public ElectricitySpotPriceMonitor(double distributionFeeWithoutVat, double sellFeeWithoutVat, double vatRate) {
        this.distributionFeeKwInclVat = distributionFeeWithoutVat * (1 + vatRate / 100) / 1000;
        this.sellFeeKwInclVat = sellFeeWithoutVat * (1 + vatRate / 100) / 1000;
        this.vatRate = vatRate;
    }

    public record Prices(IntervalPrice[] prices) {
    }

    public synchronized Prices getDayPrices() {
        try {
            Double exchangeRate = exchangeRateMonitor.getCurrentEurCzkExchangeRate();

            RawIntervalPrice[] todayPrices = getOneDayPricesImpl(0);
            RawIntervalPrice[] tomorrowPrices = getOneDayPricesImpl(1);
            RawIntervalPrice[] yesterdayPrices = (tomorrowPrices == null) ? getOneDayPricesImpl(-1) : null;

            if (exchangeRate == null || todayPrices == null || (yesterdayPrices == null && tomorrowPrices == null)) {
                // data not available (yet)
                return null;
            }

            RawIntervalPrice[] input = (tomorrowPrices != null) ? ArrayUtils.addAll(todayPrices, tomorrowPrices) : ArrayUtils.addAll(yesterdayPrices, todayPrices);

            // convert from EUR/MWh to CZK/kWh including distribution fee and VAT
            IntervalPrice[] result = new IntervalPrice[input.length];
            for (int i = 0; i < result.length; i++) {
                result[i] = createIntervalPrice(input[i], exchangeRate);
            }

            return new Prices(result);
        } catch (RuntimeException e) {
            log.error("Failed to get electricity day prices", e);
            return null;
        }
    }

    IntervalPrice createIntervalPrice(RawIntervalPrice rawIntervalPrice, double exchangeRate) {
        return new IntervalPrice(rawIntervalPrice.time, (rawIntervalPrice.price * exchangeRate) * (1 + vatRate / 100) / 1000 + distributionFeeKwInclVat, distributionFeeKwInclVat, sellFeeKwInclVat);
    }

    public synchronized IntervalPrice getPriceAt(long timeMillis) {
        Prices dayPrices = getDayPrices();
        if (dayPrices == null) {
            return null;
        }
        IntervalPrice[] prices = dayPrices.prices();
        IntervalPrice result = null;
        for (IntervalPrice price : prices) {
            if (price.time() <= timeMillis) {
                result = price;
            } else {
                break;
            }
        }
        return result;
    }

    private RawIntervalPrice[] getOneDayPricesImpl(int daysFromToday) {
        Calendar date = new GregorianCalendar();
        if (daysFromToday > 0 && date.get(Calendar.HOUR_OF_DAY) < 12) {
            // tomorrow's prices are published ~14:00, no reason to try it much earlier
            return null;
        }
        date.add(Calendar.DAY_OF_MONTH, daysFromToday);
        return cache.get(date);
    }
}
