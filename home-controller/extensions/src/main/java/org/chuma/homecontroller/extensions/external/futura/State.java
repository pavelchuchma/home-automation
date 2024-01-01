package org.chuma.homecontroller.extensions.external.futura;

import org.chuma.homecontroller.extensions.external.utils.ModbusClient;

public class State {

    private final ModbusClient client;

    public State(ModbusClient client) {
        this.client = client;
    }

    /**
     * Set ventilation speed (0 – off, 1..5 – preset
     * level 1 to 5, 6 – automatic ventilation)
     */
    public int getVentilationSpeed() {
        return client.holding.getUnsignedInt(0);
    }

    public double getAirTempAmbient() {
        return getTemperatureValue(30);
    }

    public double getAirTempFresh() {
        return getTemperatureValue(31);
    }

    public double getAirTempIndoor() {
        return getTemperatureValue(32);
    }

    public double getAirTempWaste() {
        return getTemperatureValue(33);
    }

    public int getFilterWearLevelPercent() {
        return client.input.getUnsignedInt(40);
    }

    private double getTemperatureValue(int index) {
        return client.input.getSignedInt(index) / 10d;
    }

    /**
     * Current power consumption of the unit in watts
     */
    public int getPowerConsumption() {
        return client.input.getUnsignedInt(41);
    }

    /**
     * Current value of heat recovery in watts
     */
    public int getHeatRecovering() {
        return client.input.getUnsignedInt(42);
    }

    /**
     * The CO2 value of the wall controller in ppm
     */
    public int getWallControllerCO2() {
        return client.input.getUnsignedInt(102);
    }

    /**
     * Wall controller temperature
     */
    public  double getWallControllerTemperature() {
        return getTemperatureValue(103);
    }

    public boolean getTimeProgramActive() {
        return client.holding.getUnsignedInt(12) == 1;
    }
}
