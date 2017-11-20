package packet;

import java.io.InputStream;
import java.util.Enumeration;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import org.junit.Test;

public class HeaterControllerTest {
    @Test
    public void tryConnect() {
        Enumeration portList = CommPortIdentifier.getPortIdentifiers();
        String portName = "COM5";
        SerialPort serialPort;
        int baudRate = 9600;

        while (portList.hasMoreElements()) {
            CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                if (portId.getName().equals(portName)) {
                    try {
                        serialPort = (SerialPort) portId.open("SimpleReadApp", 2000);
                        InputStream inputStream = serialPort.getInputStream();
                        serialPort.notifyOnDataAvailable(false);
                        serialPort.setSerialPortParams(baudRate,
                                SerialPort.DATABITS_8,
                                SerialPort.STOPBITS_1,
                                SerialPort.PARITY_NONE);
                        serialPort.enableReceiveTimeout(3600000);
//                        log.debug("  serial port listener started");
//                        startRead();
                        return;
                    } catch (Exception e) {
//                        log.error("Cannot open serial port", e);
//                        close();
//                        throw new PacketUartIOException(e);
                    }
                }
            }
        }
    }
}
