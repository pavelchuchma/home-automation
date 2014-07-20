package packet;

import gnu.io.*;
import node.MessageType;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PacketUartIOMock implements IPacketUartIO {
    public PacketUartIOMock() {
        //To change body of created methods use File | Settings | File Templates.
    }

    @Override
    public void addReceivedPacketListener(PacketReceivedListener listener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addSpecificReceivedPacketListener(PacketReceivedListener listener, int nodeId, int messageType) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addSentPacketListener(PacketSentListener listener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void send(Packet packet) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Packet send(Packet packet, int responseType, int timeout) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
