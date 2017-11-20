package serial.poc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class PacketFileSource implements IPacketSource {
    BufferedReader reader;

    public PacketFileSource(String file) throws FileNotFoundException {
        FileReader fr = new FileReader(file);
        reader = new BufferedReader(fr);

    }

    @Override
    public PacketData getPacket() throws InterruptedException, IOException {
        String line = reader.readLine();
        if (line != null) {
            return new PacketData(line);
        } else {
            return null;
        }
    }
}
