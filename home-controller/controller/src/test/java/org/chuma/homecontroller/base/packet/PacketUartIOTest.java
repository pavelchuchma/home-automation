package org.chuma.homecontroller.base.packet;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.node.MessageType;

public class PacketUartIOTest {
    static Logger log = LoggerFactory.getLogger(PacketUartIOTest.class.getName());
    Packet onTimer1 = new Packet(10, MessageType.MSG_OnHeartBeat, new int[]{123});

    @Test
    public void testCreation() throws Exception {
        PacketUartIO packetUartIO = new PacketUartIO("COM4", 19200);
        try {
            packetUartIO.addReceivedPacketListener(new IPacketUartIO.PacketReceivedListener() {
                @Override
                public void packetReceived(Packet packet) {
                    log.debug("packetReceived: " + packet);
                }

                @Override
                public void notifyRegistered(IPacketUartIO packetUartIO) {
                    log.debug("notifyRegistered: " + packetUartIO);
                }
            });
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

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final ScheduledFuture<?> future = scheduler.schedule(() -> {
            log.debug("Thread started");
            packetUartIO.processPacket(expResp2Bad);
            packetUartIO.processPacket(expResp1OK);
            packetUartIO.processPacket(expResp2Bad);
            log.debug("Thread ended");
        }, 500, TimeUnit.MILLISECONDS);

        Packet response = packetUartIO.send(request, MessageType.MSG_GetBuildTimeResponse, 2000);

        log.debug("Response: " + response);
        Assert.assertEquals(response, expResp1OK);
        Assert.assertNull(future.get());
    }

    @Test
    public void testListeners() throws Exception {
        final Packet[] received = new Packet[1];
        PacketUartIO.PacketReceivedListener listener = new IPacketUartIO.PacketReceivedListener() {
            @Override
            public void packetReceived(Packet packet) {
                received[0] = packet;
            }

            @Override
            public void notifyRegistered(IPacketUartIO packetUartIO) {
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