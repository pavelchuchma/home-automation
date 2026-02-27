package org.chuma.homecontroller.extensions.external.inverter.impl;

import java.io.FileReader;
import java.io.Reader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

public class WeatherForecast {
    public record Entry(
            ZonedDateTime time,
            Double airPressureAtSeaLevel,
            Double airTemperature,
            Double cloudAreaFraction,
            Double cloudAreaFractionHigh,
            Double cloudAreaFractionLow,
            Double cloudAreaFractionMedium,
            Double dewPointTemperature,
            Double fogAreaFraction,
            Double relativeHumidity,
            Double ultravioletIndexClearSky,
            Double windFromDirection,
            Double windSpeed,
            Double precipitationAmount
    ) {
    }

    public record Forecast(ZonedDateTime updatedAt, List<Entry> entries) {
    }

    public static Forecast deserialize(Reader data) throws JsonException {
        JsonObject root = (JsonObject)Jsoner.deserialize(data);
        JsonObject properties = (JsonObject)root.get("properties");
        JsonObject meta = (JsonObject)properties.get("meta");
        ZonedDateTime updatedAt = ZonedDateTime.parse((String)meta.get("updated_at"));

        JsonArray timeseries = (JsonArray)properties.get("timeseries");
        List<Entry> entries = new ArrayList<>();

        for (Object tsObj : timeseries) {
            JsonObject ts = (JsonObject)tsObj;
            ZonedDateTime time = ZonedDateTime.parse((String)ts.get("time"));

            JsonObject dataObj = (JsonObject)ts.get("data");
            JsonObject instant = (JsonObject)dataObj.get("instant");
            JsonObject details = (JsonObject)instant.get("details");
            Double precipitationAmount = null;
            JsonObject next_1_hours = (JsonObject)dataObj.get("next_1_hours");
            if (next_1_hours != null) {
                JsonObject details1 = (JsonObject)next_1_hours.get("details");
                if (details1 != null) {
                    precipitationAmount = toDouble(details1.get("precipitation_amount"));
                }
            }

            entries.add(new Entry(
                    time,
                    toDouble(details.get("air_pressure_at_sea_level")),
                    toDouble(details.get("air_temperature")),
                    toDouble(details.get("cloud_area_fraction")),
                    toDouble(details.get("cloud_area_fraction_high")),
                    toDouble(details.get("cloud_area_fraction_low")),
                    toDouble(details.get("cloud_area_fraction_medium")),
                    toDouble(details.get("dew_point_temperature")),
                    toDouble(details.get("fog_area_fraction")),
                    toDouble(details.get("relative_humidity")),
                    toDouble(details.get("ultraviolet_index_clear_sky")),
                    toDouble(details.get("wind_from_direction")),
                    toDouble(details.get("wind_speed")),
                    precipitationAmount
            ));
        }
        return new Forecast(updatedAt, entries);
    }

    private static Double toDouble(Object value) {
        return value == null ? null : ((Number)value).doubleValue();
    }

    public static Forecast deserializeFile(String filePath) {
        try (Reader reader = new FileReader(filePath)) {
            return deserialize(reader);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize " + filePath, e);
        }
    }
}
