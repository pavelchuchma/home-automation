package org.chuma.homecontroller.extensions.external.robonect.client.model;

import com.google.gson.annotations.SerializedName;

public record WeatherInfo(
        WeatherService service,
        Weather weather
) {
    public record WeatherService(boolean enable) {
    }

    public record Weather(
            @SerializedName("break")
            boolean isBreak,
            int temperature,
            int humidity,
            WeatherCondition condition
    ) {
        @SuppressWarnings("SpellCheckingInspection")
        public record WeatherCondition(
                boolean toorainy,
                boolean toocold,
                boolean toowarm,
                boolean toodry,
                boolean toowet,
                boolean day,
                boolean night
        ) {
        }
    }
}
