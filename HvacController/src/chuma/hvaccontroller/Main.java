package chuma.hvaccontroller;

import java.util.Arrays;

import chuma.hvaccontroller.debug.ConsoleOutputWriter;
import chuma.hvaccontroller.debug.PacketPrinter;
import chuma.hvaccontroller.device.FanSpeed;
import chuma.hvaccontroller.device.HvacConnector;
import chuma.hvaccontroller.device.HvacDevice;
import chuma.hvaccontroller.device.OperatingMode;
import chuma.hvaccontroller.packet.PacketConsumer;


public class Main {


    public static void main(String[] args) {

        try {
            HvacConnector connector = new HvacConnector(false);
            connector.startRead();

            IPacketProcessor packetPrinter = new PacketPrinter(new ConsoleOutputWriter(), false);
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
