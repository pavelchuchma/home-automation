package org.chuma.homecontroller.extensions.external.futura;

import java.net.UnknownHostException;

import org.apache.commons.lang3.Validate;

import org.chuma.homecontroller.extensions.external.utils.ModbusClient;

/**
 * Client for the Modbus TCP API of the Jablotron Futura recuperation unit
 * Modbus specification: doc/recuperation/futura_modbus_tcp.md
 */
public class FuturaController {
    public static final double MIN_PREFERRED_TEMP = 15;
    public static final double MAX_PREFERRED_TEMP = 30;

    private final ModbusClient modbusClient;

    public FuturaController(String ipAddress) throws UnknownHostException {
        modbusClient = new ModbusClient(ipAddress, 502, false, 3000,
                new int[][]{{0, 21}, {30, 38}, {40, 52}, {60, 75}, {100, 154}, {160, 165}},
                new int[][]{{0, 17}},
                new int[][]{});
    }

    State getState() {
        modbusClient.refreshData();
        return new State(modbusClient);
    }

    /**
     * Set ventilation speed (0 – off, 1..5 – preset
     * level 1 to 5, 6 – automatic ventilation)
     */
    public void setVentilationSpeed(int speed) {
        Validate.inclusiveBetween(0, 6, speed);
        writeRegister(0, speed);
    }

    public void setTimeProgramActive(boolean value) {
        writeRegister(12, (value) ? 1 : 0);
    }

    /**
     * Runs the function for the given time, 0 ends it.
     */
    public void startTimedFunction(TimedFunction function, int seconds) {
        Validate.inclusiveBetween(0, function.maxSeconds, seconds);
        writeRegister(function.register, seconds);
    }

    public void setPreferredTemperature(double temp) {
        Validate.inclusiveBetween(MIN_PREFERRED_TEMP, MAX_PREFERRED_TEMP, temp);
        writeRegister(10, (int)Math.round(temp * 10));
    }

    /**
     * @param humidity in %: dry = 25, comfort = 50, humid = 75
     */
    public void setPreferredHumidity(int humidity) {
        Validate.isTrue(humidity == 25 || humidity == 50 || humidity == 75, "Unsupported humidity %d", humidity);
        writeRegister(11, humidity * 10);
    }

    public void setBypassEnabled(boolean value) {
        writeRegister(14, (value) ? 1 : 0);
    }

    protected void writeRegister(int address, int value) {
        modbusClient.writeMultipleRegisterValue(address, value);
    }
}
