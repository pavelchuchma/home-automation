package org.chuma.homecontroller.extensions.external.futura;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.BiFunction;

import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FuturaController {
    private final TCPMasterConnection connection;
    protected static Logger log = LoggerFactory.getLogger(FuturaController.class.getName());

    public FuturaController(String ipAddress) throws UnknownHostException {
        connection = new TCPMasterConnection(InetAddress.getByName(ipAddress));
        connection.setPort(502);
    }

    State getState() throws Exception {
        int[] inputRegisters = new int[167];
        readInputRegisters(0, 22, inputRegisters);
        readInputRegisters(30, 9, inputRegisters);
        readInputRegisters(40, 13, inputRegisters);
        readInputRegisters(60, 16, inputRegisters);
        readInputRegisters(100, 55, inputRegisters);
        readInputRegisters(160, 6, inputRegisters);

        int[] holdingRegisters = new int[19];
        readHoldingRegisters(0, 18, holdingRegisters);
        return new State(inputRegisters, holdingRegisters);
    }

    private void readInputRegisters(int ref, int count, int[] target) {
        readRegisters(ref, count, target, new ReadInputRegistersRequest(ref, count),
                (response, i) -> ((ReadInputRegistersResponse)response).getRegisterValue(i));
    }

    private void readHoldingRegisters(int ref, int count, int[] target) {
        readRegisters(ref, count, target, new ReadMultipleRegistersRequest(ref, count),
                (response, i) -> ((ReadMultipleRegistersResponse)response).getRegisterValue(i));
    }

    private void readRegisters(int ref, int count, int[] target,
                               ModbusRequest req, BiFunction<ModbusResponse, Integer, Integer> f) {
        ModbusResponse res = doModbusCall(req);
        for (int i = 0; i < count; i++) {
            target[ref + i] = f.apply(res, i);
        }
    }

    private synchronized ModbusResponse doModbusCall(ModbusRequest req) {
        try {
            if (!connection.isConnected()) {
                log.debug("Connecting to futura: {}:{}", connection.getAddress().getHostAddress(), connection.getPort());
                connection.connect();
            }
            ModbusTCPTransaction trans = new ModbusTCPTransaction(connection);
            req.setUnitID(1);
            trans.setRequest(req);
            trans.execute();
            return trans.getResponse();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set ventilation power (0 – off, 1..5 – preset
     * level 1 to 5, 6 – automatic ventilation)
     */
    public void setVentilationSpeed(int speed) {
        Validate.inclusiveBetween(0, 6, speed);
        writeRegisterValue(0, speed);
    }

    public void setTimeProgramActive(boolean value) {
        writeRegisterValue(12, (value) ? 1 : 0);
    }

    private void writeRegisterValue(int address, int speed) {
        doModbusCall(new WriteMultipleRegistersRequest(address, new Register[]{new SimpleRegister(speed)}));
    }
}
