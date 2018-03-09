package serial.poc;

import java.util.Arrays;

import serial.poc.Packet.FanSpeed;
import serial.poc.Packet.OperatingMode;
import serial.poc.Packet.PacketConsumer;
import serial.poc.device.HvacDevice;


public class Main {


    public static void main(String[] args) {

        try {
            HvacConnector connector = new HvacConnector();
            connector.startRead();

            IPacketProcessor packetPrinter = new PacketPrinter(new ConsoleOutputWriter());
            HvacDevice hvacDevice = new HvacDevice(connector);
            PacketConsumer packetConsumer = new PacketConsumer(Arrays.asList(packetPrinter, hvacDevice.getProcessor()));

            String cmd = (args.length > 0) ? args[0] : "test";
            new Thread(() -> {
                schedule(connector, hvacDevice, cmd);
            }).start();
            packetConsumer.consume(connector);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void schedule(HvacConnector connector, HvacDevice device, String cmd) {
        try {
            switch (cmd) {
                case "test":
                    Thread.sleep(5000);
                    device.set(true, OperatingMode.FAN, FanSpeed.SPEED_1, 25, false, false);
                    Thread.sleep(10000);
                    device.set(true, OperatingMode.FAN, FanSpeed.SPEED_2, 25, false, false);
                    Thread.sleep(10000);
                    device.set(false, OperatingMode.HEAT, FanSpeed.SPEED_1, 23, false, false);
                    break;
                case "on":
                    Thread.sleep(2000);
                    device.set(true, OperatingMode.HEAT, FanSpeed.SPEED_1, 23, false, false);
                    break;
                case "on4":
                    Thread.sleep(2000);
                    device.set(true, OperatingMode.HEAT, FanSpeed.SPEED_2, 23, false, false);
                    break;

                case "on5":
                    Thread.sleep(2000);
                    device.set(true, OperatingMode.HEAT, FanSpeed.SPEED_3, 23, false, false);
                    break;

                case "off":
                    Thread.sleep(2000);
                    device.set(false, OperatingMode.HEAT, FanSpeed.SPEED_1, 23, false, false);
                    break;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
