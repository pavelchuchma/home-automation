package packet;

import node.MessageType;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class PacketUartIOTest {
    Packet onTimer1 = new Packet(10, MessageType.MSG_OnHeartBeat, new int[]{123});
    static Logger log = Logger.getLogger(PacketUartIOTest.class.getName());

    @Test
    public void testCreation() throws Exception {
        PacketUartIO packetUartIO = new PacketUartIO("COM4", 19200);
        try {
            packetUartIO.addReceivedPacketListener(new ReceivedPacketHandler());
            System.out.println("Listening ...");

            packetUartIO.processPacket(onTimer1);

            System.out.println("done.");
        } finally {
            packetUartIO.close();
        }
    }

    @Test
    public void testRequestResponse() throws Exception {
        final PacketUartIO packetUartIO = new PacketUartIO("COM4", 19200);
        Packet request = Packet.createMsgGetBuildTime(123);
        final Packet expResp1OK = new Packet(123, MessageType.MSG_GetBuildTimeResponse, new int[]{1, 2, 3, 4, 5});
        final Packet expResp2Bad = new Packet(100, MessageType.MSG_GetBuildTimeResponse, new int[]{1, 2, 3, 4, 5});
        new Thread(new Runnable() {
            @Override
            public void run() {
                log.debug("Thread started");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                packetUartIO.processPacket(expResp2Bad);
                packetUartIO.processPacket(expResp1OK);
                packetUartIO.processPacket(expResp2Bad);
                log.debug("Thread ended");
            }
        }).start();

        Packet response = packetUartIO.send(request, MessageType.MSG_GetBuildTimeResponse, 2000);

        log.debug("Response: " + response);
        Assert.assertEquals(response, expResp1OK);
    }

    @Test
    public void testListeners() throws Exception {
        final Packet[] received = new Packet[1];
        PacketUartIO.PacketReceivedListener listener = new PacketUartIO.PacketReceivedListener() {
            @Override
            public void packetReceived(Packet packet) {
                received[0] = packet;
            }

            @Override
            public void notifyRegistered(PacketUartIO packetUartIO) {
            }
        };


        final Packet expResp1OK = new Packet(123, MessageType.MSG_GetBuildTimeResponse, new int[]{1, 2, 3, 4, 5});

        {
            final PacketUartIO packetUartIO = new PacketUartIO("COM4", 19200);
            packetUartIO.addSpecificReceivedPacketListener(listener, 123, MessageType.MSG_GetBuildTimeResponse);
            received[0] = null;
            packetUartIO.processPacket(expResp1OK);
            Assert.assertEquals(expResp1OK, received[0]);
            packetUartIO.close();
        }
        {
            final PacketUartIO packetUartIO = new PacketUartIO("COM4", 19200);
            packetUartIO.addSpecificReceivedPacketListener(listener, 123, -1);
            received[0] = null;
            packetUartIO.processPacket(expResp1OK);
            Assert.assertEquals(expResp1OK, received[0]);
            packetUartIO.close();
        }
        {
            final PacketUartIO packetUartIO = new PacketUartIO("COM4", 19200);
            packetUartIO.addSpecificReceivedPacketListener(listener, 124, -1);
            received[0] = null;
            packetUartIO.processPacket(expResp1OK);
            Assert.assertNull(received[0]);
            packetUartIO.close();
        }
        {
            final PacketUartIO packetUartIO = new PacketUartIO("COM4", 19200);
            packetUartIO.addSpecificReceivedPacketListener(listener, 123, MessageType.MSG_EchoResponse);
            received[0] = null;
            packetUartIO.processPacket(expResp1OK);
            Assert.assertNull(received[0]);
            packetUartIO.close();
        }
    }
}