package serial.poc.device;

import java.io.IOException;

import serial.poc.HvacConnector;
import serial.poc.IPacketProcessor;
import serial.poc.Packet.FanSpeed;
import serial.poc.Packet.Get52ResponsePacket;
import serial.poc.Packet.Get53ResponsePacket;
import serial.poc.Packet.Get54ResponsePacket;
import serial.poc.Packet.OperatingMode;
import serial.poc.Packet.Packet;
import serial.poc.Packet.SetPacketRequest;

public class HvacDevice {

    public static final int ADDR_THIS_CONTROLLER = 0x85;
    public static final int ADDR_HVAC_DEVICE = 0x20;
    private final HvacConnector connector;
    private boolean running;
    private FanSpeed fanSpeed;
    private OperatingMode currentMode;
    private OperatingMode targetMode;
    private boolean autoMode;
    private boolean quiteMode;
    private boolean sleepMode;
    private int targetTemperature;
    private int airTemperature;
    private boolean x;
    private boolean y;

    public HvacDevice(HvacConnector connector) {
        this.connector = connector;
    }

    private static boolean selectBoolean(Boolean input, boolean current) {
        return (input != null) ? input : current;
    }

    public IPacketProcessor getProcessor() {
        return new IPacketProcessor() {
            @Override
            public void start() throws IOException {
                System.out.println("Starting HvacDevice");
            }

            @Override
            public void stop() throws IOException {

            }

            @Override
            public void process(Packet packet) throws IOException {
                if (packet instanceof Get52ResponsePacket) {
                    Get52ResponsePacket get52Resp = (Get52ResponsePacket) packet;
                    running = get52Resp.isOn();
                    fanSpeed = get52Resp.getFanSpeed();
                    currentMode = get52Resp.getMode();
                    autoMode = get52Resp.isModeAuto();
                    targetTemperature = get52Resp.getTargetTemperature();
                    airTemperature = get52Resp.getAirTemperature();
                    x = get52Resp.isX();
                    y = get52Resp.isY();
                } else if (packet instanceof Get53ResponsePacket) {
                    Get53ResponsePacket get53Resp = (Get53ResponsePacket) packet;
                    targetMode = get53Resp.getMode();
                    sleepMode = get53Resp.isSleepMode();
                } else if (packet instanceof Get54ResponsePacket) {
                    Get54ResponsePacket get54Resp = (Get54ResponsePacket) packet;
                    quiteMode = get54Resp.isQuite();
                }

                if (packet.getCommand() == 0xD1) {
                    System.out.println("State: " + HvacDevice.this.toString());
                }
            }
        };
    }

    public void set(Boolean on, OperatingMode mode, FanSpeed fanSpeed, int temp, Boolean sleep, Boolean quite) {
        SetPacketRequest setPacketRequest = new SetPacketRequest(
                ADDR_THIS_CONTROLLER,
                ADDR_HVAC_DEVICE,
                selectBoolean(on, running),
                (mode != OperatingMode.NONE) ? mode : currentMode,
                (fanSpeed != FanSpeed.NONE) ? fanSpeed : this.fanSpeed,
                (temp > 0) ? temp : this.targetTemperature,
                selectBoolean(sleep, sleepMode),
                selectBoolean(quite, quiteMode)
        );

        System.out.println("Sending: " + setPacketRequest);
        connector.sendData(setPacketRequest.getData());
    }


    private int boolAsInt(boolean b) {
        return (b) ? 1 : 0;
    }

    @Override
    public String toString() {
        return String.format("on:%d mode:%s(%s) fan:%s tgtTemp:%d airTemp:%d auto:%d sleep:%d quite:%d x:%d y:%d",
                boolAsInt(running), targetMode, currentMode, fanSpeed, targetTemperature, airTemperature, boolAsInt(autoMode), boolAsInt(sleepMode), boolAsInt(quiteMode), boolAsInt(x), boolAsInt(y));
    }
}
