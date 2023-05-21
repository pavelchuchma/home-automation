package org.chuma.homecontroller.base.packet;

import java.io.IOException;

import com.fazecast.jSerialComm.SerialPort;


public class PacketUartIO extends AbstractPacketUartIO {
    PacketSerializer packetSerializer;
    AbstractSerialPortConnection serialPortConnection;

    public PacketUartIO(String portName, int baudRate) {
        packetSerializer = new PacketSerializer();
        serialPortConnection = new AbstractSerialPortConnection(portName) {
            @Override
            protected void initializePort() throws IOException {
                if (serialPort.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY)
                        && serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 3600_000, 0)) {
                    log.debug("Serial port '[{}] {}' configured.", serialPort.getSystemPortPath(), serialPort.getDescriptivePortName());
                } else {
                    throw new IOException("Failed to set configure port " + serialPort.getDescriptivePortName());
                }
            }

            @Override
            protected void readImpl() throws IOException {
                Packet receivedPacket = packetSerializer.readPacket(inputStream);
                processPacket(receivedPacket);
            }
        };
    }

    @Override
    protected void sendImpl(Packet packet) throws IOException {
        PacketSerializer.writePacket(packet, serialPortConnection.outputStream);
    }

    @Override
    public void start() throws IOException {
        serialPortConnection.start();
    }

    @Override
    public void close() {
        serialPortConnection.stop();
    }
}
