package chuma.hvaccontroller.debug;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import chuma.hvaccontroller.IPacketSource;
import chuma.hvaccontroller.packet.PacketData;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class PacketFileSource implements IPacketSource {
    private final String file;
    private final Object startSyncObj = new Object();
    private BufferedReader reader;

    public PacketFileSource(String file) throws FileNotFoundException {
        this.file = file;
    }

    @Override
    public PacketData getPacket() {
        try {
            while (reader == null) {
                synchronized (startSyncObj) {
                    startSyncObj.wait();
                }
            }
            String line = reader.readLine();
            if (line != null) {
                return new PacketData(line);
            }
        } catch (IOException e) {
            // ignore it
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void startRead() throws IOException {
        FileReader fr = new FileReader(file);
        reader = new BufferedReader(fr);
        synchronized (startSyncObj) {
            startSyncObj.notify();
        }
    }

    @Override
    public void sendData(PacketData data) {
        throw new NotImplementedException();
    }
}
