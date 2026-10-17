package org.chuma.homecontroller.extensions.external.boiler;

import java.net.UnknownHostException;
import java.util.ArrayList;

import com.ghgande.j2mod.modbus.ModbusIOException;
import com.ghgande.j2mod.modbus.ModbusSlaveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.extensions.external.utils.ModbusClient;

public class BoilerController {

    static class StatusFlags {
        public static final int on = 1;
        public static final int hot = 1 << 1;
        public static final int eHeat = 1 << 2;
        public static final int pump = 1 << 3;
        public static final int vacation = 1 << 4;
    }

    static class ModbusRegisters {
        public static final int iregDisplayMode = 100;
        public static final int iregStatusAge = 101;
        public static final int iregStatusFlags = 102;
        public static final int iregTempT5U = 103;
        public static final int iregTempT5L = 104;
        public static final int iregTempT3 = 105;
        public static final int iregTempT4 = 106;
        public static final int iregTempTP = 107;
        public static final int iregTempTh = 108;
        // diagnostics of the last failed key sequence in the gateway: KeySequence ordinal, step, State.MODE ordinal
        public static final int iregLastFailSequence = 109;
        public static final int iregLastFailStep = 110;
        public static final int iregLastFailMode = 111;

        public static final int cregRefreshStatus = 200;
        public static final int cregPowerOn = 210;
        public static final int cregEHeatOn = 211;
        public static final int hregTempTarget = 300;
        public static final int hregPressKey = 310;
    }

    static class Key {
        // format: colum * 16 + row
        public static final int keyEHeater = 0x00;
        public static final int keyVacation = 0x01;
        public static final int keyDisinfect = 0x02;
        public static final int keyEHeaterPlusDisinfect = 0x03;
        public static final int keyUpArrow = 0x10;
        public static final int keyEnter = 0x11;
        public static final int keyDownArrow = 0x12;
        public static final int keyClockTimer = 0x20;
        public static final int keyCancel = 0x21;
        public static final int keyOnOff = 0x22;
    }

    private static final Logger log = LoggerFactory.getLogger(BoilerController.class.getName());
    // gateway key sequences take up to 15 s and a request arriving while one runs waits for it
    private static final int MODBUS_TIMEOUT_MS = 30_000;
    private static final int WRITE_ATTEMPTS = 3;
    // the gateway rejected the write after its key sequence failed; the display just needs to settle
    private static final long RETRY_DELAY_AFTER_REJECT_MS = 5_000;
    private static final long RETRY_DELAY_AFTER_IO_ERROR_MS = 15_000;

    private final ModbusClient modbusClient;

    BoilerController(String address) throws UnknownHostException {
        modbusClient = new ModbusClient(address, 502, false, MODBUS_TIMEOUT_MS,
                new int[][]{{ModbusRegisters.iregDisplayMode, ModbusRegisters.iregLastFailMode}},
                new int[][]{{ModbusRegisters.hregTempTarget, ModbusRegisters.hregTempTarget}},
                new int[][]{});
    }

    State getState() {
        log.debug("Refreshing state");
        modbusClient.refreshData();

        int statusFlags = modbusClient.input.getUnsignedInt(ModbusRegisters.iregStatusFlags);
        State state = new State(
                State.MODE.values()[modbusClient.input.getUnsignedInt(ModbusRegisters.iregDisplayMode) & 0xFF],
                getStatusAge(),
                getTemp(ModbusRegisters.iregTempT5U),
                getTemp(ModbusRegisters.iregTempT5L),
                getTemp(ModbusRegisters.iregTempT3),
                getTemp(ModbusRegisters.iregTempT4),
                getTemp(ModbusRegisters.iregTempTP),
                getTemp(ModbusRegisters.iregTempTh),
                getTempFromHoldReg(ModbusRegisters.hregTempTarget),
                ((statusFlags & StatusFlags.on) != 0),
                ((statusFlags & StatusFlags.hot) != 0),
                ((statusFlags & StatusFlags.eHeat) != 0),
                ((statusFlags & StatusFlags.pump) != 0),
                ((statusFlags & StatusFlags.vacation) != 0),
                readLastFailure());

        if (log.isDebugEnabled()) {
            ArrayList<String> flags = new ArrayList<>();
            if (state.isOn()) flags.add("on");
            if (state.isHot()) flags.add("hot");
            if (state.isEHeat()) flags.add("eHeat");
            if (state.isPump()) flags.add("pump");
            if (state.isVacation()) flags.add("vacation");

            log.debug("Mode: {}, statusAge: {}, tSU: {}, tSL: {}, tT3: {}, tT4: {}, tTP: {}, tTh: {}, tTarget: {}, flags[{}], lastFail: {}",
                    state.getDisplayMode(), state.getStatusAge(), state.getTempT5U(), state.getTempT5L(), state.getTempT3(),
                    state.getTempT4(), state.getTempTP(), state.getTempTh(), state.getTargetTemp(), String.join(", ", flags),
                    state.getLastFailure());
        }
        return state;
    }

    private State.LastFailure readLastFailure() {
        return new State.LastFailure(
                enumValue(KeySequence.values(), modbusClient.input.getUnsignedInt(ModbusRegisters.iregLastFailSequence)),
                modbusClient.input.getUnsignedInt(ModbusRegisters.iregLastFailStep),
                enumValue(State.MODE.values(), modbusClient.input.getUnsignedInt(ModbusRegisters.iregLastFailMode)));
    }

    private static <E> E enumValue(E[] values, int ordinal) {
        return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : null;
    }

    private int getTemp(int index) {
        return modbusClient.input.getUnsignedInt(index) - 128;
    }

    private int getTempFromHoldReg(int index) {
        return modbusClient.holding.getUnsignedInt(index) - 128;
    }

    private int getStatusAge() {
        return modbusClient.input.getUnsignedInt(ModbusRegisters.iregStatusAge);
    }

    public void pressKey(int key, int durationMs) {
        log.debug("Pressing key {}", key);
        modbusClient.writeSingleRegisterValue(ModbusRegisters.hregPressKey, (key << 8) + (durationMs / 100));
    }

    public void refreshStatus() {
        log.debug("Refreshing status");
        writeWithRetry("refresh status", () -> modbusClient.writeCoilRegister(ModbusRegisters.cregRefreshStatus, true));
    }

    public void setPowerOn(boolean value) {
        log.debug("Setting power on: {}", value);
        writeWithRetry("set power on " + value, () -> modbusClient.writeCoilRegister(ModbusRegisters.cregPowerOn, value));
    }

    public void setTargetTemp(int targetTemp) {
        log.debug("Setting target temp to {}", targetTemp);
        writeWithRetry("set target temp " + targetTemp,
                () -> modbusClient.writeSingleRegisterValue(ModbusRegisters.hregTempTarget, targetTemp + 128));
    }

    public void turnEHeatOn() {
        log.debug("Turning EHeat on");
        writeWithRetry("turn eHeat on", () -> modbusClient.writeCoilRegister(ModbusRegisters.cregEHeatOn, true));
    }

    /**
     * Every gateway write is a self-verifying "ensure state" key sequence, so repeating a rejected
     * write is safe: a key press that did land makes the next attempt a no-op on the gateway side.
     */
    private void writeWithRetry(String description, Runnable write) {
        for (int attempt = 1; ; attempt++) {
            try {
                write.run();
                if (attempt > 1) {
                    log.warn("Boiler {} succeeded on attempt {}/{}", description, attempt, WRITE_ATTEMPTS);
                }
                return;
            } catch (RuntimeException e) {
                Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                boolean ioError = cause instanceof ModbusIOException;
                if (attempt >= WRITE_ATTEMPTS || !(ioError || cause instanceof ModbusSlaveException)) {
                    throw e;
                }
                long delayMs = ioError ? RETRY_DELAY_AFTER_IO_ERROR_MS : RETRY_DELAY_AFTER_REJECT_MS;
                log.warn("Boiler {} failed (attempt {}/{}): {}; gateway lastFail: {}; next attempt in {} ms",
                        description, attempt, WRITE_ATTEMPTS, cause, readLastFailureQuietly(ioError), delayMs);
                sleep(delayMs);
            }
        }
    }

    private String readLastFailureQuietly(boolean gatewayUnreachable) {
        if (gatewayUnreachable) {
            // a diagnostics read would only wait for another timeout
            return "not read";
        }
        try {
            modbusClient.refreshData();
            return readLastFailure().toString();
        } catch (Exception e) {
            return "unavailable (" + e.getMessage() + ")";
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for the next attempt", e);
        }
    }
}
