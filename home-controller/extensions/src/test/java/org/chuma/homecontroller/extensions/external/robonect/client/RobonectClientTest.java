package org.chuma.homecontroller.extensions.external.robonect.client;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.fail;

import org.chuma.homecontroller.base.utils.Options;
import org.chuma.homecontroller.extensions.external.robonect.client.model.MowerInfo;
import org.chuma.homecontroller.extensions.external.robonect.client.model.WeatherInfo;

public class RobonectClientTest {
    private static RobonectEndpoint robonectEndpoint;

    @BeforeClass
    public static void beforeClass() {
        robonectEndpoint = getRobonectEndpoint();
    }

    public static RobonectEndpoint getRobonectEndpoint() {
        Options options = new Options("../cfg/app.properties", "default-app.properties");
        return new RobonectEndpoint(options.get("robonect.host"),
                options.get("robonect.username"), options.get("robonect.password"));
    }

    @Test
    public void testAA() {
        RobonectClient client = new RobonectClient(robonectEndpoint);
        try {
            WeatherInfo weatherInfo = client.getWeatherInfo();
//            VersionInfo versionInfo = client.getVersionInfo();
//            GpsInfo gpsInfo = client.getGpsInfo();
//            Gps gps = gpsInfo.getGps();
//            double latitude = gps.getLatitude();
//            double longitude = gps.getLongitude();
            MowerInfo mowerInfo = client.getMowerInfo();
//            RobonectAnswer answer = client.setMode(ModeCommand.Mode.HOME);
//            assertTrue(answer.isSuccessful());

            return;
        } catch (Exception e) {
            fail("Exception occurred: " + e.getMessage());
        }
    }


}