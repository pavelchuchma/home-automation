package org.chuma.homecontroller.extensions.external.utils;

import java.net.UnknownHostException;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusClientTest extends TestCase {
    enum MODE {
        unknown,
        displayOff,
        locked,
        invalid,
        infoT5U,
        infoT5L,
        infoT3,
        infoT4,
        infoTP,
        infoTh,
        infoCE,
        infoER1,
        infoER2,
        infoER3,
        infoD7F,
        setClock,
        setTemp,
        unlocked,
        setVacation,
        vacation
    }

    static class StatusFlags {
        public static final int on = 1;
        public static final int hot = 1 << 1;
        public static final int eheat = 1 << 2;
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

    private static final Logger log = LoggerFactory.getLogger(ModbusClientTest.class.getName());


    public void testConnect() throws UnknownHostException, InterruptedException {
        ModbusClient modbusClient = createModbusClient();

        while (true) {
            try {
                refreshStatus(modbusClient);
                modbusClient.refreshData();
//                setTargetTemp(modbusClient, 58);
//                setTargetTemp(modbusClient, 57);
//                setTargetTemp(modbusClient, 54);

                int displayMode = modbusClient.input.getUnsignedInt(ModbusRegisters.iregDisplayMode);
                MODE mode = MODE.values()[displayMode & 0xFF];
                int statusFlags = modbusClient.input.getUnsignedInt(ModbusRegisters.iregStatusFlags);
                log.debug("Mode: " + mode + ", " +
                        "statusAge: " + getStatusAge(modbusClient) + ", " +
                        "tSU: " + getTemp(modbusClient, ModbusRegisters.iregTempT5U) + ", " +
                        "tSL: " + getTemp(modbusClient, ModbusRegisters.iregTempT5L) + ", " +
                        "tT3: " + getTemp(modbusClient, ModbusRegisters.iregTempT3) + ", " +
                        "tT4: " + getTemp(modbusClient, ModbusRegisters.iregTempT4) + ", " +
                        "tTP: " + getTemp(modbusClient, ModbusRegisters.iregTempTP) + ", " +
                        "tTh: " + getTemp(modbusClient, ModbusRegisters.iregTempTh) + ", " +
                        "tTarget: " + getTempFromHoldReg(modbusClient, ModbusRegisters.hregTempTarget) + ", " +
                        "on: " + ((statusFlags & StatusFlags.on) != 0) + ", " +
                        "eHeat: " + ((statusFlags & StatusFlags.eheat) != 0) + ", " +
                        "pump: " + ((statusFlags & StatusFlags.pump) != 0)
                );
            } catch (RuntimeException e) {
                log.error("failure", e);
            }
            Thread.sleep(1_000);
        }
    }

    private static ModbusClient createModbusClient() throws UnknownHostException {
        return new ModbusClient("boiler.local", 502, false, 20000,
                new int[][]{{ModbusRegisters.iregDisplayMode, ModbusRegisters.iregTempTh}},
                new int[][]{{ModbusRegisters.hregTempTarget, ModbusRegisters.hregTempTarget}},
                new int[][]{});
    }

    private static int getTemp(ModbusClient modbusClient, int index) {
        return modbusClient.input.getUnsignedInt(index) - 128;
    }

    private static int getTempFromHoldReg(ModbusClient modbusClient, int index) {
        return modbusClient.holding.getUnsignedInt(index) - 128;
    }

    private static int getStatusAge(ModbusClient modbusClient) {
        return modbusClient.input.getUnsignedInt(ModbusRegisters.iregStatusAge);
    }

    void pressKey(ModbusClient modbusClient, int key, int durationMs) {
        modbusClient.writeSingleRegisterValue(ModbusRegisters.hregPressKey, (key << 8) + (durationMs / 100));
    }

    void refreshStatus(ModbusClient modbusClient) {
        modbusClient.writeCoilRegister(ModbusRegisters.cregRefreshStatus, true);
    }

    void setPowerOn(ModbusClient modbusClient, boolean value) {
        modbusClient.writeCoilRegister(ModbusRegisters.cregPowerOn, value);
    }

    void setTargetTemp(ModbusClient modbusClient, int targetTemp) {
        modbusClient.writeSingleRegisterValue(ModbusRegisters.hregTempTarget, targetTemp + 128);
    }

    public void testCoil() throws UnknownHostException {
        ModbusClient modbusClient = createModbusClient();

        modbusClient.writeSingleRegisterValue(ModbusRegisters.hregPressKey, (Key.keyCancel << 8) + 30);
        modbusClient.writeCoilRegister(ModbusRegisters.cregRefreshStatus, true);
        modbusClient.writeCoilRegister(200, false);

    }
}