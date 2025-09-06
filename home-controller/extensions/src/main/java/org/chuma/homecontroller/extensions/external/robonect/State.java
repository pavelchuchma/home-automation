package org.chuma.homecontroller.extensions.external.robonect;

import org.chuma.homecontroller.extensions.external.robonect.client.model.Blades;
import org.chuma.homecontroller.extensions.external.robonect.client.model.ErrorEntry;
import org.chuma.homecontroller.extensions.external.robonect.client.model.Gps;
import org.chuma.homecontroller.extensions.external.robonect.client.model.Health;
import org.chuma.homecontroller.extensions.external.robonect.client.model.Status;
import org.chuma.homecontroller.extensions.external.robonect.client.model.Timer;
import org.chuma.homecontroller.extensions.external.robonect.client.model.WeatherInfo;
import org.chuma.homecontroller.extensions.external.robonect.client.model.Wlan;

public record State(
        Status status,
        Timer timer,
        Wlan wlan,
        Health health,
        Blades blades,
        ErrorEntry error,
        Gps gps,
        WeatherInfo.Weather weather
) {
}
