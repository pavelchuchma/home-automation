package org.chuma.homecontroller.base.packet;

import java.util.Arrays;

import org.apache.commons.lang3.Validate;

import org.chuma.homecontroller.base.node.MessageType;

/**
 * Packet represent single message. It's properties include source/target node ID,
 * message type and data, at most five bytes.
 */
public class Packet {
    public static final int MAX_PWM_VALUE = 48;
    private static final int[] ECHO_DATA = new int[]{'A', 'B', 'C', 'D', 'E'};

    public final int nodeId;
    public final int messageType;
    public final int[] data;
    public final int length;

    public Packet(int nodeId, int messageType, int[] data) {
        if (data != null && data.length > 5) throw new IllegalArgumentException("Too long data: " + data.length);
        this.nodeId = (byte) nodeId;
        this.messageType = (byte) messageType;
        this.data = data;
        length = (data != null) ? 2 + data.length : 2;
    }

    public static Packet createMsgUartTransmitPerfTestRequest(int nodeId, int packetCount, int packetLength, int firstByte, boolean waitForFreeOutput) {
        return new Packet(nodeId, MessageType.MSG_UartTransmitPerfTestRequest, new int[]{packetCount, packetLength, firstByte, (byte) ((waitForFreeOutput) ? 1 : 0)});
    }

    public static Packet createMsgReadRamRequest(int nodeId, int address) {
        return new Packet(nodeId, MessageType.MSG_ReadRamRequest, new int[]{address & 0xFF, (address >> 8) & 0xFF});
    }

    public static Packet createMsgWriteRamRequest(int nodeId, int address, int mask, int value) {
        return new Packet(nodeId, MessageType.MSG_WriteRamRequest, new int[]{address & 0xFF, (address >> 8) & 0xFF, mask, value});
    }

    public static Packet createMsgEchoRequest(int nodeId, int dataLen) {
        return new Packet(nodeId, MessageType.MSG_EchoRequest, Arrays.copyOf(ECHO_DATA, dataLen));
    }

    public static Packet createMsgEchoRequest(int nodeId, int a, int b) {
        return new Packet(nodeId, MessageType.MSG_EchoRequest, new int[]{a, b, a + 1, a + 2, a + 3});
    }

    public static Packet createMsgGetBuildTime(int nodeId) {
        return new Packet(nodeId, MessageType.MSG_GetBuildTimeRequest, null);
    }

    public static Packet createMsgSetPort(int nodeId, char port, int valueMask, int value, int eventMask, int trisValue) {
        if (port < 'A' || port > 'D')
            throw new IllegalArgumentException("Only ports A, B, C and D are valid");
        int[] data;
        if (eventMask < 0) {
            data = new int[]{valueMask, value};
        } else if (trisValue < 0) {
            data = new int[]{valueMask, value, eventMask};
        } else {
            data = new int[]{valueMask, value, eventMask, trisValue};
        }
        return new Packet(nodeId, MessageType.MSG_SetPortA + port - 'A', data);
    }

    public static Packet createMsgEnablePwmRequest(int nodeId, int cpuFrequency, int canBaudRatePrescaler, int value) {
        return new Packet(nodeId, MessageType.MSG_EnablePwmRequest, new int[]{1, cpuFrequency & 0xFF, canBaudRatePrescaler & 0xFF, value & 0xFF});
    }

    public static Packet createMsgSetPwmValueRequest(int nodeId, int value) {
        return new Packet(nodeId, MessageType.MSG_SetPwmValueRequest, new int[]{1, value & 0xFF});
    }

    public static Packet createMsgInitializationFinished(int nodeId) {
        return new Packet(nodeId, MessageType.MSG_InitializationFinished, null);
    }

    public static Packet createMsgSetHeartBeatPeriod(int nodeId, int seconds) {
        return new Packet(nodeId, MessageType.MSG_SetHeartBeatPeriod, new int[]{seconds});
    }

    public static Packet createMsgMSGSetManualPwmValue(int nodeId, char port, int pin, int value) {
        int portNum = port - 'A';
        Validate.inclusiveBetween('A', 'C', port, "Invalid port value");
        Validate.inclusiveBetween(0, 7, pin, "Invalid pin value");
        Validate.inclusiveBetween(0, MAX_PWM_VALUE, value, "Invalid pwm value");
        return new Packet(nodeId, MessageType.MSG_SetManualPwmValueRequest, new int[]{portNum + (pin << 4), value});
    }

    public static Packet createMsgSetFrequency(int nodeId, int cpuFrequency) {
        return new Packet(nodeId, MessageType.MSG_SetFrequencyRequest, new int[]{cpuFrequency & 0xFF, (cpuFrequency - 1) & 0xFF});
    }

    public static Packet createMsgReset(int nodeId) {
        return new Packet(nodeId, MessageType.MSG_ResetRequest, null);
    }

    public static Packet createMsgReadProgramMemory(int nodeId, int address) {
        return new Packet(nodeId, MessageType.MSG_ReadProgramRequest, new int[]{address & 0xFF, (address >> 8) & 0xFF, (address >> 16) & 0xFF});
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Packet packet = (Packet) o;

        if (length != packet.length) return false;
        if (messageType != packet.messageType) return false;
        if (nodeId != packet.nodeId) return false;
        if (!Arrays.equals(data, packet.data)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) nodeId;
        result = 31 * result + (int) messageType;
        result = 31 * result + (data != null ? Arrays.hashCode(data) : 0);
        result = 31 * result + length;
        return result;
    }

    @Override
    public String toString() {
        return String.format("{nodeId=%d, messageType=%s, data=%s}", nodeId, MessageType.toString(messageType), Arrays.toString(data));
    }
}
