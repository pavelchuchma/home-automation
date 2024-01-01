package org.chuma.homecontroller.extensions.external.futura;

import java.net.UnknownHostException;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.extensions.external.utils.ModbusClient;

public class FuturaController {
    protected static Logger log = LoggerFactory.getLogger(FuturaController.class.getName());
    private final ModbusClient modbusClient;

    public FuturaController(String ipAddress) throws UnknownHostException {
        modbusClient = new ModbusClient(ipAddress, 502, false,
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
        modbusClient.writeMultipleRegisterValue(0, speed);
    }

    public void setTimeProgramActive(boolean value) {
        modbusClient.writeMultipleRegisterValue(12, (value) ? 1 : 0);
    }
}
