package org.chuma.homecontroller.extensions.external.boiler;

import java.net.UnknownHostException;
import java.util.ArrayList;

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

        public static final int cregRefreshStatus = 200;
        public static final int cregPowerOn = 210;
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
    private final ModbusClient modbusClient;

    BoilerController(String address) throws UnknownHostException {
        modbusClient = new ModbusClient(address, 502, false, 20000,
                new int[][]{{ModbusRegisters.iregDisplayMode, ModbusRegisters.iregTempTh}},
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
                ((statusFlags & StatusFlags.vacation) != 0));

        if (log.isDebugEnabled()) {
            ArrayList<String> flags = new ArrayList<>();
            if (state.isOn()) flags.add("on");
            if (state.isHot()) flags.add("hotn");
            if (state.isEHeat()) flags.add("eHeat");
            if (state.isPump()) flags.add("pump");
            if (state.isVacation()) flags.add("vacation");

            log.debug("Mode: {}, statusAge: {}, tSU: {}, tSL: {}, tT3: {}, tT4: {}, tTP: {}, tTh: {}, tTarget: {}, flags[{}]",
                    state.getDisplayMode(), state.getStatusAge(), state.getTempT5U(), state.getTempT5L(), state.getTempT3(),
                    state.getTempT4(), state.getTempTP(), state.getTempTh(), state.getTargetTemp(), String.join(", ", flags));
        }
        return state;
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
        modbusClient.writeCoilRegister(ModbusRegisters.cregRefreshStatus, true);
    }

    public void setPowerOn(boolean value) {
        log.trace("Setting power on: {}", value);
        modbusClient.writeCoilRegister(ModbusRegisters.cregPowerOn, value);
    }

    public void setTargetTemp(int targetTemp) {
        log.debug("Setting target temp to {}", targetTemp);
        modbusClient.writeSingleRegisterValue(ModbusRegisters.hregTempTarget, targetTemp + 128);
    }
}
