package org.chuma.homecontroller.extensions.external.inverter;

import junit.framework.TestCase;

public class ElectricitySpotPriceMonitorTest extends TestCase {
    public void testGetPrice() {
        ElectricitySpotPriceMonitor monitor = new ElectricitySpotPriceMonitor();
        ElectricitySpotPriceMonitor.Prices dayPrices = monitor.getDayPrices();
        ElectricitySpotPriceMonitor.Prices dayPrices2 = monitor.getDayPrices();
        return;
    }
}