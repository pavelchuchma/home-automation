package org.chuma.homecontroller.extensions.external.futura;

import org.chuma.homecontroller.extensions.external.utils.ModbusClient;

/**
 * Register addresses: doc/recuperation/futura_modbus_tcp.md
 */
public class State {

    private final ModbusClient.Registers input;
    private final ModbusClient.Registers holding;

    public State(ModbusClient client) {
        this(client.input, client.holding);
    }

    State(ModbusClient.Registers input, ModbusClient.Registers holding) {
        this.input = input;
        this.holding = holding;
    }

    /**
     * Set ventilation speed (0 – off, 1..5 – preset
     * level 1 to 5, 6 – automatic ventilation)
     */
    public int getVentilationSpeed() {
        return holding.getUnsignedInt(0);
    }

    public double getAirTempAmbient() {
        return getTenthsValue(30);
    }

    public double getAirTempFresh() {
        return getTenthsValue(31);
    }

    public double getAirTempIndoor() {
        return getTenthsValue(32);
    }

    public double getAirTempWaste() {
        return getTenthsValue(33);
    }

    public double getHumidityAmbient() {
        return getTenthsValue(34);
    }

    public double getHumidityFresh() {
        return getTenthsValue(35);
    }

    public double getHumidityIndoor() {
        return getTenthsValue(36);
    }

    public double getHumidityWaste() {
        return getTenthsValue(37);
    }

    /**
     * Bits of active operation modes (fut_mode)
     */
    public long getModeFlags() {
        return input.getUnsignedInt32(16);
    }

    /**
     * Bits of active errors (fut_error)
     */
    public long getErrorFlags() {
        return input.getUnsignedInt32(18);
    }

    /**
     * Bits of active warnings (fut_warning)
     */
    public long getWarningFlags() {
        return input.getUnsignedInt32(20);
    }

    public int getFilterWearLevelPercent() {
        return input.getUnsignedInt(40);
    }

    private double getTenthsValue(int index) {
        return input.getSignedInt(index) / 10d;
    }

    /**
     * Current power consumption of the unit in watts
     */
    public int getPowerConsumption() {
        return input.getUnsignedInt(41);
    }

    /**
     * Current value of heat recovery in watts
     */
    public int getHeatRecovering() {
        return input.getUnsignedInt(42);
    }

    /**
     * Sum of supply and exhaust air flow in m3/h
     */
    public int getAirFlow() {
        return input.getUnsignedInt(44);
    }

    public int getFanPowerSupplyPercent() {
        return input.getUnsignedInt(45);
    }

    public int getFanPowerExhaustPercent() {
        return input.getUnsignedInt(46);
    }

    public int getFanRpmSupply() {
        return input.getUnsignedInt(47);
    }

    public int getFanRpmExhaust() {
        return input.getUnsignedInt(48);
    }

    /**
     * The CO2 value of the wall controller in ppm
     */
    public int getWallControllerCO2() {
        return input.getUnsignedInt(102);
    }

    /**
     * Wall controller temperature
     */
    public  double getWallControllerTemperature() {
        return getTenthsValue(103);
    }

    public double getWallControllerHumidity() {
        return input.getUnsignedInt(104) / 10d;
    }

    public boolean getTimeProgramActive() {
        return holding.getUnsignedInt(12) == 1;
    }

    /**
     * Remaining time of the function in seconds, 0 when it is not running
     */
    public int getRemainingSeconds(TimedFunction function) {
        return holding.getUnsignedInt(function.register);
    }

    public double getPreferredTemperature() {
        return holding.getUnsignedInt(10) / 10d;
    }

    /**
     * Preferred humidity in %: dry = 25, comfort = 50, humid = 75
     */
    public int getPreferredHumidity() {
        return (int)Math.round(holding.getUnsignedInt(11) / 10d);
    }

    public boolean getBypassEnabled() {
        return holding.getUnsignedInt(14) == 1;
    }
}
