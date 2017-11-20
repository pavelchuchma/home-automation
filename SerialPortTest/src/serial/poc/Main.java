package serial.poc;

import java.util.Enumeration;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;


public class Main {


    public static void main(String[] args) {

        StreamPacketSource packetSource = openPort();
        packetSource.startRead();

        try {
//        IPacketConsumer packetConsumer = new PacketFileWriter();
            IPacketConsumer packetConsumer = new PacketPrinter(new ConsoleOutputWriter());
            packetConsumer.consume(packetSource);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static StreamPacketSource openPort() {
        Enumeration portList = CommPortIdentifier.getPortIdentifiers();
        String portName = "/dev/ttyUSB0";
        SerialPort serialPort;
        int baudRate = 9600 / 4;

//        System.out.print("AAAA" + (char)27 + "[34;43mBlue text with yellow background" + ((char)27 + "[0m"));
//        System.out.print("AAAA" + yellow("XX") + "BBB");


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
                        serialPort.enableReceiveTimeout(600000);
//                        log.debug("  serial port listener started");

                        return new StreamPacketSource(serialPort.getInputStream());

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

}
