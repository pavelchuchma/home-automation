package org.chuma.homecontroller.base.packet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.SerialPort;

public class PacketUartIO extends AbstractPacketUartIO {
    private SerialPort serialPort;
    private boolean closed = false;

    public PacketUartIO(String portName, int baudRate) throws PacketUartIOException {
        log.debug("Creating '{}' @{} bauds...", portName, baudRate);
        Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();

        while (portList.hasMoreElements()) {
            CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                if (portId.getName().equals(portName)) {
                    try {
                        serialPort = (SerialPort) portId.open("SimpleReadApp", 2000);
                        serialPort.notifyOnDataAvailable(false);
                        serialPort.setSerialPortParams(baudRate,
                                SerialPort.DATABITS_8,
                                SerialPort.STOPBITS_1,
                                SerialPort.PARITY_NONE);
                        serialPort.enableReceiveTimeout(3600000);
                        log.debug("  serial port listener started");
                        return;
                    } catch (Exception e) {
                        log.error("Cannot open serial port", e);
                        close();
                        throw new PacketUartIOException(e);
                    }
                }
            }
        }
        throw new PacketUartIOException(new NoSuchPortException());
    }

    @Override
    public void start() throws IOException {
        InputStream inputStream = serialPort.getInputStream();
        new Thread(() -> {
            log.debug("Starting read com thread");
            PacketSerializer packetSerializer = new PacketSerializer();
            while (!closed) {
                try {
                    Packet receivedPacket = packetSerializer.readPacket(inputStream);
                    processPacket(receivedPacket);
                } catch (IOException e) {
                    msgLog.error("receiveError", e);
                    log.error("receiveError", e);
                    e.printStackTrace();
                }
            }
            log.debug("Ending read com thread");
        }, "ComRead").start();
    }

    @Override
    protected void sendImpl(Packet packet) throws IOException {
        PacketSerializer.writePacket(packet, serialPort.getOutputStream());
    }

    @Override
    public void close() {
        closed = true;
        if (serialPort != null) {
            serialPort.close();
            log.debug("Serial port listener closed");
        }
    }
}
