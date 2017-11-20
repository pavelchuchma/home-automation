package serial.poc;

import java.io.IOException;
import java.util.Arrays;

import static java.lang.String.format;

public class PacketData {
    public int[] data;
    public int[] rawData;
    public int readTime;
    public int from;
    public int to;
    public int command;

    public PacketData(int[] rawData, int readTime) throws IOException {
        initialize(rawData, readTime);
    }

    /**
     * From raw line
     */
    public PacketData(String line) throws IOException {
        String[] parts = line.trim().split(" ");
        int readTime = Integer.parseInt(parts[0]);
        int[] rawData = new int[parts.length - 1];
        for (int i = 0; i < rawData.length; i++) {
            rawData[i] = Integer.parseInt(parts[i + 1], 16);
        }
        initialize(rawData, readTime);
    }

    public static void appendFormattedNumber(StringBuilder sb, int num) {
        sb.append(String.format("%02X", num));
//        if (num < 10) {
//            sb.append("  ");
//        } else if (num < 100) {
//            sb.append(" ");
//        }
//        sb.append(num);
    }

    public static String dataArrayToString(int[] data) {
        return dataArrayToString(data, null);
    }

    public static String dataArrayToString(int[] data, int[] previous) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            int b = data[i];
            if (previous != null && previous[i] != b) {
                sb.append(colorizeDiffValue(b));
            } else {
                appendFormattedNumber(sb, b);
            }
            if (i + 1 < data.length)
                sb.append(" ");
        }
        return sb.toString();
    }

    private static String colorizeCommand(int c) {
        return (char) 27 + "[34;43m" + String.format("%02X", c) + (char) 27 + "[0m";
    }

    private static String colorizeDiffValue(int c) {
        return (char) 27 + "[31;40m" + String.format("%02X", c) + (char) 27 + "[0m";
    }

    public boolean isRequest() {
        return from == 0x84;
    }

    private void initialize(int[] rawData, int readTime) throws IOException {
        this.readTime = readTime;
        this.rawData = rawData;


        int crc = 0;
        for (int i = 1; i < rawData.length - 2; i++) {
            crc ^= rawData[i];
        }

        if (rawData.length < 3) {
            throw new IOException(format("Packet is too short: [%3d]: %s", readTime, dataArrayToString(rawData)));
        }

        if (crc != rawData[rawData.length - 2]) {
            throw new IOException(format("CRC Error: [%3d]: %s", readTime, dataArrayToString(rawData)));
        }

        if (rawData[0] == 50 && rawData[rawData.length - 1] == 52) {
            from = rawData[1];
            to = rawData[2];
            command = rawData[3];
            data = Arrays.copyOfRange(rawData, 4, rawData.length - 2);
        } else {
            from = to = -1;
            data = rawData;
        }
    }

    public String toString(PacketData previous) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(String.format("%3d", readTime));
        sb.append("): ");
        sb.append("'").append(colorizeCommand(command)).append("' ");
        sb.append(dataArrayToString(data, (previous != null) ? previous.data : null));
        return sb.toString();
    }

    public String toRawString() {
        return String.format("%4d %s", readTime, dataArrayToString(rawData));
    }
}
