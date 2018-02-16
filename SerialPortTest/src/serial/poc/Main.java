package serial.poc;

import java.io.IOException;
import java.util.Enumeration;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;


public class Main {


    public static void main(String[] args) {

        HvacConnector connector = openPort();
        connector.startRead();
        try {
//        IPacketConsumer packetConsumer = new PacketFileWriter();
            IPacketConsumer packetConsumer = new PacketPrinter(new ConsoleOutputWriter());
            new Thread(() -> {
                schedule(connector);
            }).start();
            packetConsumer.consume(connector);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void schedule(HvacConnector connector) {
        try {
            Thread.sleep(6000);
            System.out.println("SENDING ON!");
            PacketData turnOnPacket = new PacketData(0x85, 0x20, 0xA0, new int[]{0x1F, 0x18, 0x57, 0x04, 0xF4, 0x00, 0x00, 0x00});
            connector.sendData(turnOnPacket);
            System.out.println("ON SENT!");

            Thread.sleep(8000);
            System.out.println("SENDING OFF!");
            PacketData turnOffPacket = new PacketData(0x85, 0x20, 0xA0, new int[]{0x1F, 0x18, 0x57, 0x04, 0xC4, 0x00, 0x00, 0x00});
            connector.sendData(turnOffPacket);
            System.out.println("OFF SENT!");
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public static HvacConnector openPort() {
        Enumeration portList = CommPortIdentifier.getPortIdentifiers();
        String portName = isWindows() ? "COM5" : "/dev/ttyUSB0";
        SerialPort serialPort;
        int baudRate = 9600 / 4;

        while (portList.hasMoreElements()) {
            CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
            System.out.println("Checking: " + portId.getName());

            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                if (portId.getName().equals(portName)) {
                    System.out.println("Found: " + portId.getName());

                    try {
                        serialPort = (SerialPort) portId.open("SimpleReadApp", 2000);
                        serialPort.notifyOnDataAvailable(false);
                        serialPort.setSerialPortParams(baudRate,
                                SerialPort.DATABITS_8,
                                SerialPort.STOPBITS_1,
                                SerialPort.PARITY_EVEN);
                        serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
                        serialPort.setOutputBufferSize(0);
                        serialPort.setInputBufferSize(0);
//                        serialPort.setLowLatency();
                        serialPort.enableReceiveTimeout(600000);

                        return new HvacConnector(serialPort.getInputStream(), serialPort.getOutputStream());

                    } catch (Exception e) {
//                        log.error("Cannot open serial port", e);
//                        close();
//                        throw new PacketUartIOException(e);
                    }
                }
            }
        }

        return null;
    }

    static boolean isWindows() {
        return System.getenv("COMPUTERNAME") != null;
    }

}
