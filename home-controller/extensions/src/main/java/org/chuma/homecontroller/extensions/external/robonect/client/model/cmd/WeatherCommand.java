package org.chuma.homecontroller.extensions.external.robonect.client.model.cmd;

/**
 * Queries the weather.
 */
public class WeatherCommand implements Command {
    @Override
    public String toCommandURL(String baseURL) {
        return baseURL + "?cmd=weather";
    }
}
