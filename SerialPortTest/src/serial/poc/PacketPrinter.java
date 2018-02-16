package serial.poc;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import serial.poc.Packet.AbstractSetPacket;
import serial.poc.Packet.Packet;
import serial.poc.Packet.PacketFactory;
import serial.poc.Packet.UnknownPacket;

public class PacketPrinter implements IPacketConsumer {
    IOutputWriter outputWriter;
    FileWriter fileWriter;

    Packet currentRequest;
    Packet currentResponse;
    Packet requestToDiff;
    Packet responseToDiff;

    private Map<String, Packet> previousPackets = new HashMap<>();
    private int count = 0;

    public PacketPrinter(IOutputWriter outputWriter) throws IOException {
        this.outputWriter = outputWriter;
        String now = new SimpleDateFormat("yyyyMMdd_hh-mm-ss").format(new Date());
        fileWriter = new FileWriter("raw-" + now + ".log");
    }

    private String createKey(Packet packet) {
        if (packet == null) {
            return "null";
        }
        PacketData packetData = packet.getData();
        String val = String.format("%d->%d:%d", packetData.from, packetData.to, packetData.command);
        if (packetData.isRequest() && (packetData.command == 0x70 || packetData.command == 0x71)) {
            val += "-" + packetData.data[0];
        }
        return val;
    }

    private String createKey(Packet response, Packet request) {
        return createKey(response) + "_" + createKey(request);
    }

    @Override
    public void consume(IPacketSource reader) throws IOException, InterruptedException {
        outputWriter.open();
        while (true) {
            PacketData packetData = reader.getPacket();
            if (packetData == null) {
                break;
            }
            process(packetData, reader);
        }
        outputWriter.close();
    }

    public void process(PacketData packetData, IPacketSource reader) throws IOException {
        fileWriter.write(packetData.toRawString() + "\n");
        fileWriter.flush();

        Packet packet = PacketFactory.Deserialize(packetData);


        if (packet.getTo() == 0xAD) {
            // ignore request to 'AD'
//            return;
/*
            if (count++ == 5 && reader instanceof StreamPacketSource) {
                System.out.println("WAITING TO SEND!");
                try {
                    Thread.sleep(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("SENDING!");
                ((StreamPacketSource)reader).sendData();
                System.out.println("SENT!");
            }
*/
        }

        String mapKey;
        if (packet.isRequest()) {
            printPair();

            currentRequest = packet;
            currentResponse = null;

            mapKey = createKey(packet);
            requestToDiff = previousPackets.put(mapKey, packet);
        } else {
            currentResponse = packet;
            mapKey = createKey(currentRequest, packet);
            responseToDiff = previousPackets.put(mapKey, packet);
        }
    }

    private void printPair() throws IOException {
        if (currentRequest == null) {
            if (currentResponse != null) {
//                throw new IllegalStateException();
            }
            return;
        }
        printPacketData(currentRequest, requestToDiff);
        if (currentResponse != null) {
            outputWriter.append("  -->  ");
            printPacketData(currentResponse, responseToDiff);
        }
        outputWriter.append("\n");

        if (!(currentRequest instanceof UnknownPacket) || !(currentResponse instanceof UnknownPacket)) {
            printPacketInfo(currentRequest);
            if (currentResponse != null) {
                outputWriter.append("       ");
                printPacketInfo(currentResponse);
            }
            outputWriter.append("\n");
        }
    }

    private void printPacketInfo(Packet packet) throws IOException {
        int len = 0;
        if (!(packet instanceof UnknownPacket)) {
            String name = packet.getClass().getSimpleName();
            outputWriter.append(name + ": ");
            len = name.length() + 2;
        }
        String[] split = packet.toString().split(";");
        for (int i = 0; i < split.length; i++) {
            if (!split[i].isEmpty()) {
                outputWriter.appendColorized(split[i], i);
                outputWriter.append(" ");
                len += split[i].length() + 1;
            }
        }

        for (int i = len; i < 74; i++) {
            outputWriter.append(" ");
        }
    }

    private void printPacketData(Packet packet, Packet previousPacket) throws IOException {
//        printReadTime(packet.readTime);
//        w.append(" ");
        writeCommand(packet);
        for (int i = 0; i < packet.getData().data.length; i++) {
            outputWriter.append(" ");
            appendDataByte(packet, i, previousPacket);
        }
    }

    private void appendDataByte(Packet packet, int index, Packet previousPacket) throws IOException {
        int val = packet.getData().data[index];

        String fontColor = "black";

        int mask = packet.getUnderstandMask()[index];
        for (int bit = 7; bit >= 0; bit--) {
            boolean isUnderstood = (mask & (1 << bit)) != 0;
            int valBit = getBitValue(val, bit);
            if (isUnderstood) {
                outputWriter.appendColorized(Integer.toString(valBit), index);
            } else {
                String backgroundColor;
                if (previousPacket != null && getBitValue(previousPacket.getData().data[index], bit) != valBit) {
                    backgroundColor = "red";
                } else {
                    backgroundColor = "white";
                }
                outputWriter.appendColorized(Integer.toString(valBit), fontColor, backgroundColor);
            }
        }
    }

    private int getBitValue(int val, int index) {
        return (val & (1 << index)) >> index;
    }

    private void writeCommand(Packet packet) throws IOException {
        String backgroundColor;
        if (packet instanceof AbstractSetPacket) {
            backgroundColor = "orange";
        } else {
            backgroundColor = "lightblue";
        }
        outputWriter.appendColorized(String.format("%02X", packet.getCommand()), "black", backgroundColor);
    }

//    void printReadTime(int readTime) throws IOException {
//        w.append(String.format("<i>%4d</i>", readTime));
//    }
}
