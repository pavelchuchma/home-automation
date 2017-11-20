package serial.poc.Packet;

import serial.poc.PacketData;

public class Get52ResponsePacket extends AbstractPacket {
    public static final int MASK_TARGET_TEMP = 0x3F;
    public static final int MASK_FAN_SPEED = 0x07;
    public static final int MASK_MODE = 0x0F;
    public static final int MASK_MODE_AUTO = 0x20;
    public static final int MASK_ON = 0x80;

    public Get52ResponsePacket(PacketData packetData) {
        super(packetData);
    }

    @Override
    public int[] getUnderstandMask() {
        return new int[]{
                MASK_TARGET_TEMP,
                0,
                0,
                MASK_FAN_SPEED,
                MASK_ON | MASK_MODE | MASK_MODE_AUTO,
                0,
                0,
                0
        };
    }

    FanSpeed getFanSpeed() {
        int val = packetData.data[3] & MASK_FAN_SPEED;
        return AbstractSetPacket.getFanSpeedImpl(val);
    }

    OperatingMode getMode() {
        int value = packetData.data[4] & MASK_MODE;

        if ((value & 0x01) != 0) {
            return OperatingMode.HEAT;
        }
        if ((value & 0x02) != 0) {
            return OperatingMode.COOL;
        }
        if ((value & 0x04) != 0) {
            return OperatingMode.DRY;
        }
        if ((value & 0x08) != 0) {
            return OperatingMode.FAN;
        }
        throw new IllegalArgumentException("Unknown mode: " + packetData.data[3]);
    }

    boolean isModeAuto() {
        return (packetData.data[4] & MASK_MODE_AUTO) != 0;
    }

    public int getTargetTemperature() {
        return (packetData.data[0] & MASK_TARGET_TEMP) + 9;
    }

    boolean isOn() {
        return (packetData.data[4] & MASK_ON) != 0;
    }

    @Override
    public String toString() {
        return String.format("temp:%d;;;fan:%s;on:%d auto:%d mode:%s;", getTargetTemperature(), getFanSpeed(), boolAsInt(isOn()), boolAsInt(isModeAuto()), getMode());
    }
}