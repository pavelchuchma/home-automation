package serial.poc;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PacketFileWriter implements IPacketConsumer {
    private Map<String, PacketData> previousPackets = new HashMap<>();
    private HashMap<String, Integer> occurrences = new HashMap<>();

    @Override
    public void consume(IPacketSource reader) {
        try {
            String now = new SimpleDateFormat("yyyyMMdd_hh-mm-ss").format(new Date());
            FileWriter w = new FileWriter("raw-" + now + ".log");
            while (true) {
                try {
                    PacketData packetData = reader.getPacket();
                    w.write(packetData.toRawString() + "\n");
                    w.flush();

                    if (packetData.from == 132 && packetData.to == 173) {
                        // ignore "50,132,173,209,  1,  0,  0,  0,  0,  0,  0,  0,249, 52"
                        continue;
                    }
                    String mapKey = packetData.command + "-" + packetData.from + "->" + packetData.to;
                    PacketData previous = previousPackets.get(mapKey);
                    previousPackets.put(mapKey, packetData);
                    if (packetData.from == 132) {
                        System.out.println();
                    } else if (packetData.from == 32) {
                        System.out.print(" -> ");
                    }
                    if (packetData.from >= 0) {
                        System.out.printf("[%3d] %s", getOccurrence(packetData), packetData.toString(previous)/*Packet.dataArrayToString(packet.data)*/);
                    }

//            System.out.println(packet.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private int getOccurrence(PacketData p) {
        String key = p.from + "-" + p.to + PacketData.dataArrayToString(p.data);
        Integer i = occurrences.get(key);
        i = (i == null) ? 1 : i + 1;

        occurrences.put(key, i);
        return i;
    }
}
