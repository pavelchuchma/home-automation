package serial.poc.Packet;

import serial.poc.PacketData;

public class SetPacketRequest extends AbstractSetPacket {
    public SetPacketRequest(PacketData data) {
        super(data);
    }

    public SetPacketRequest(int from, int to, boolean on, OperatingMode mode, FanSpeed fanSpeed, int temp, boolean sleep, boolean quite) {
        super(from, to, PacketType.CMD_SET, on, mode, fanSpeed, temp, sleep, quite);
    }
}
