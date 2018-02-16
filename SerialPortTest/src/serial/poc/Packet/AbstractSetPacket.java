package serial.poc.Packet;

import serial.poc.PacketData;

public abstract class AbstractSetPacket extends AbstractPacket {

    public static final int MASK_SLEEP = 0x20;
    public static final int MASK_QUITE = 0x20;
    public static final int MASK_MODE = 0x07;
    public static final int MASK_FAN_SPEED = 0xE0;
    public static final int MASK_TARGET_TEMP = 0x1F;
    public static final int MASK_ON = 0x10;

    public AbstractSetPacket(PacketData data) {
        super(data);
    }

    public static FanSpeed getFanSpeedImpl(int val) {
        switch (val) {
            case 0x00:
                return FanSpeed.AUTO;
            case 0x02:
                return FanSpeed.SPEED_1;
            case 0x04:
                return FanSpeed.SPEED_2;
            case 0x05:
                return FanSpeed.SPEED_3;
            default:
                throw new IllegalArgumentException("Unexpected mode number: " + val);
        }
    }

    public static OperatingMode getOperatingModeImpl(int val) {
        switch (val) {
            case 0x00:
                return OperatingMode.AUTO;
            case 0x01:
                return OperatingMode.COOL;
            case 0x02:
                return OperatingMode.DRY;
            case 0x03:
                return OperatingMode.FAN;
            case 0x04:
                return OperatingMode.HEAT;
            default:
                throw new IllegalArgumentException("Unexpected mode number: " + val);
        }
    }

    public FanSpeed getFanSpeed() {
        int val = (packetData.data[2] & MASK_FAN_SPEED) >> 5;
        return getFanSpeedImpl(val);
    }

    boolean isSleep() {
        return (packetData.data[0] & MASK_SLEEP) != 0;
    }

    boolean isQuite() {
        return (packetData.data[6] & MASK_QUITE) != 0;
    }

    boolean isOn() {
        return (packetData.data[4] & MASK_ON) != 0;
    }

    OperatingMode getMode() {
        int val = packetData.data[3] & MASK_MODE;
        return getOperatingModeImpl(val);
    }

    public int getTargetTemperature() {
        return (packetData.data[2] & MASK_TARGET_TEMP);
    }

    @Override
    public String toString() {
        return String.format("sleep:%d;;temp:%d fan:%s;mode:%s;on:%d;;quite:%d;", boolAsInt(isSleep()), getTargetTemperature(), getFanSpeed(), getMode(), boolAsInt(isOn()), boolAsInt(isQuite()));
    }

    @Override
    public int[] getUnderstandMask() {
        return new int[]{
                MASK_SLEEP,
                0,
                MASK_TARGET_TEMP | MASK_FAN_SPEED,
                MASK_MODE,
                MASK_ON,
                0,
                MASK_QUITE,
                0};
    }
}
