package org.chuma.homecontroller.base.packet;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortIOException;

import org.chuma.homecontroller.base.utils.Utils;


public class PacketUartIO extends AbstractPacketUartIO {
    private SerialPort serialPort;

    private InputStream inputStream = null;
    private String portName;
    private int baudRate;
    private boolean closed = false;

    public PacketUartIO(String portName, int baudRate) {
        this.portName = portName;
        this.baudRate = baudRate;
    }

    private void initializePort() throws IOException {
        log.debug("Initializing serial port '{}' @{} bauds...", portName, baudRate);
        String portList = String.join(", ",
                Arrays.stream(SerialPort.getCommPorts()).map(sp -> "[" + sp.getSystemPortPath() + "] " + sp.getDescriptivePortName())
                        .toArray(CharSequence[]::new));
        log.info("Existing serial ports: {}", portList);
        if (serialPort != null) {
            serialPort.closePort();
        }

        serialPort = SerialPort.getCommPort(portName);

        if (!serialPort.openPort()) {
            throw new IOException("Failed to open port " + portName + ", existing ports are: " + portList);
        }

        if (serialPort.setBaudRate(baudRate)
                && serialPort.setNumDataBits(8)
                && serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT)
                && serialPort.setParity(SerialPort.NO_PARITY)
                && serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 3600_000, 0)) {
            log.debug("Serial port '[{}] {}' configured.", serialPort.getSystemPortPath(), serialPort.getDescriptivePortName());
        } else {
            throw new IOException("Failed to set configure port " + serialPort.getDescriptivePortName());
        }
        inputStream = serialPort.getInputStream();
    }

    @Override
    public void start() {
        new Thread(() -> {
            log.debug("Starting read com thread");
            PacketSerializer packetSerializer = new PacketSerializer();
            while (!closed) {
                while (inputStream == null) {
                    try {
                        initializePort();
                    } catch (Exception e) {
                        log.error("Port initialization failed, going to sleep for a moment", e);
                        Utils.sleep(10_000);
                    }
                }

                try {
                    Packet receivedPacket = packetSerializer.readPacket(inputStream);
                    processPacket(receivedPacket);
                } catch (SerialPortIOException | EOFException e) {
                    inputStream = null;
                } catch (IOException e) {
                    msgLog.error("receiveError", e);
                    log.error("receiveError", e);
                    log.error("serial port: {}", serialPort.getSystemPortPath());
                    log.error("serial port baud rate: {}", serialPort.getBaudRate());
                    log.error("serial port open: {}", serialPort.isOpen());
                    log.error("serial port stop bits: {}", serialPort.getNumStopBits());
                    log.error("serial port data bits: {}", serialPort.getNumDataBits());
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
            serialPort.closePort();
            log.debug("Serial port listener closed");
        }
    }
}
