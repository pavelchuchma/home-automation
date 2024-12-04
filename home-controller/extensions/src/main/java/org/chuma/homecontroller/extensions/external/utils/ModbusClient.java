package org.chuma.homecontroller.extensions.external.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteCoilRequest;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusClient {
    private final TCPMasterConnection connection;
    private final boolean closeConnection;
    private boolean closeConnectionSuspended = false;
    private static final Logger log = LoggerFactory.getLogger(ModbusClient.class.getName());
    private final int[][] inputRegisterRanges;
    private final int[][] holdingRegisterRanges;
    private final int[][] stableHoldingRegisterRanges;

    private boolean stableRegistersAreRead = false;

    public final Registers input;
    public final Registers holding;

    /**
     * @param ipAddress                   device ip address or host name
     * @param port                        modbus port, usually 502
     * @param closeConnection             close connection after each call (if not closeConnectionSuspended is not set)
     * @param timeoutMs                   tcp timeout
     * @param inputRegisterRanges         array of {start position, length} pairs of Input registers to be read
     * @param holdingRegisterRanges       array of {start position, length} pairs of Holding registers to be read
     * @param stableHoldingRegisterRanges array of {start position, length} pairs of Holding registers to be read only once (no change is expected)
     */
    public ModbusClient(String ipAddress, int port, boolean closeConnection, int timeoutMs, int[][] inputRegisterRanges, int[][] holdingRegisterRanges,
                        int[][] stableHoldingRegisterRanges) throws UnknownHostException {
        connection = new TCPMasterConnection(InetAddress.getByName(ipAddress));
        connection.setPort(port);
        connection.setTimeout(timeoutMs);
        this.closeConnection = closeConnection;

        this.inputRegisterRanges = inputRegisterRanges;
        this.holdingRegisterRanges = holdingRegisterRanges;
        this.stableHoldingRegisterRanges = stableHoldingRegisterRanges;

        input = new Registers(validateAndAllocateRanges(inputRegisterRanges));
        holding = new Registers(validateAndAllocateRanges(ArrayUtils.addAll(holdingRegisterRanges, stableHoldingRegisterRanges)));
    }

    int[] validateAndAllocateRanges(int[][] ranges) {
        int maxIndex = 0;
        for (int[] r : ranges) {
            Validate.isTrue(r.length == 2);
            Validate.isTrue(r[0] <= r[1]);
            maxIndex = Math.max(maxIndex, r[1]);
        }
        int[] data = new int[maxIndex + 1];
        Arrays.fill(data, Integer.MIN_VALUE);
        return data;
    }

    public void refreshData() {
        long startTime = 0;
        if (log.isTraceEnabled()) {
            startTime = System.currentTimeMillis();
        }
        closeConnectionSuspended = true;
        try {
            for (int[] r : inputRegisterRanges) {
                readInputRegisters(r[0], r[1] - r[0] + 1, input.data);
            }
//            log.trace("reading holding after {} ms", System.currentTimeMillis() - startTime);
            for (int[] r : holdingRegisterRanges) {
                readHoldingRegisters(r[0], r[1] - r[0] + 1, holding.data);
            }
            if (!stableRegistersAreRead) {
//                log.trace("reading stable holding after {} ms", System.currentTimeMillis() - startTime);
                for (int[] r : stableHoldingRegisterRanges) {
                    readHoldingRegisters(r[0], r[1] - r[0] + 1, holding.data);
                }
                stableRegistersAreRead = true;
            }
        } finally {
            if (closeConnection) {
                closeConnectionSuspended = false;
                connection.close();
            }
        }
        log.trace("Refresh done in {} ms", System.currentTimeMillis() - startTime);
    }

    private void readInputRegisters(int ref, int count, int[] target) {
        ReadInputRegistersResponse res = (ReadInputRegistersResponse)doModbusCall(new ReadInputRegistersRequest(ref, count));
        Validate.validState(res.getByteCount() == 2 * count, "Unexpected response length: %d != %d", res.getByteCount(), 2 * count);
        for (int i = 0; i < count; i++) {
            target[ref + i] = res.getRegisterValue(i);
        }
    }

    private void readHoldingRegisters(int ref, int count, int[] target) {
        ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse)doModbusCall(new ReadMultipleRegistersRequest(ref, count));
        Validate.validState(res.getByteCount() == 2 * count, "Unexpected response length: %d != %d", res.getByteCount(), 2 * count);
        for (int i = 0; i < count; i++) {
            target[ref + i] = res.getRegisterValue(i);
        }
    }

    private synchronized ModbusResponse doModbusCall(ModbusRequest req) {
        long startTime = 0;
        if (log.isTraceEnabled()) {
            startTime = System.currentTimeMillis();
        }
        try {
            ModbusTCPTransaction trans = new ModbusTCPTransaction(connection);
            req.setUnitID(1);
            trans.setReconnecting(closeConnection && !closeConnectionSuspended);
            trans.setRequest(req);
            trans.setRetries(0);
            trans.execute();
            return trans.getResponse();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            log.trace("call done in {} ms", System.currentTimeMillis() - startTime);
        }
    }

    public void writeMultipleRegisterValue(int address, int value) {
        log.debug("writing multiple register: {}={}", String.format("0x%04X", address), value);
        doModbusCall(new WriteMultipleRegistersRequest(address, new Register[]{new SimpleRegister(value)}));
    }

    public void writeSingleRegisterValue(int address, int value) {
        log.debug("writing single register: {}={}", String.format("0x%04X", address), value);
        doModbusCall(new WriteSingleRegisterRequest(address, new SimpleRegister(value)));
    }

    public void writeCoilRegister(int address, boolean value) {
        log.debug("writing coil register: {}={}", String.format("0x%04X", address), value);
        doModbusCall(new WriteCoilRequest(address, value));
    }

    public static class Registers {
        private final int[] data;

        public Registers(int[] data) {
            this.data = data;
        }

        public String getString(int startIndex, int len) {
            char[] res = new char[2 * len];
            for (int i = 0; i < len; i++) {
                int v = data[startIndex + i];
                res[2 * i] = (char)((v >> 8) & 0xFF);
                res[2 * i + 1] = (char)(v & 0xFF);
            }
            return new String(res);
        }

        public int getUnsignedInt(int index) {
            return data[index];
        }

        public int getSignedInt(int index) {
            return (short)data[index];
        }
    }
}
