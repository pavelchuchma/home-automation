package node;

import com.sun.corba.se.impl.interceptors.PICurrent;
import com.sun.javaws.exceptions.InvalidArgumentException;
import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Test;
import packet.Packet;
import packet.PacketUartIO;

import java.io.IOException;
import java.util.Date;

public class NodeTest {
    static Logger log = Logger.getLogger(NodeTest.class.getName());

    @Test
    public void testNodeCreation() {
        Node n1 = new Node(10, null, "00001111", "00000011", "11110000", "11110000");

        Assert.assertEquals(10, n1.getNodeId());
        Assert.assertEquals(15, n1.getPortCTris());
        Assert.assertEquals(3, n1.getPortCEventMask());
        Assert.assertEquals(240, n1.getPortCValueMask());
        Assert.assertEquals(240, n1.getPortCValue());
    }

    @Test
    public void testReadRam() throws Exception {
        PacketUartIO packetUartIO = new PacketUartIO("COM1", 19200);
        Node node = new Node(1, packetUartIO);

        log.debug("node.Node " + node.nodeId + " val: " + node.readMemory(0x43));
        packetUartIO.close();
    }

    @Test
    public void testGetBuildDate() throws Exception {
        PacketUartIO packetUartIO = new PacketUartIO("COM1", 19200);

        for (int i = 1; i <= 2; i++) {
            Node node = new Node(i, packetUartIO);

            Date buildTime = node.getBuildTime();
            log.debug("node.Node " + node.nodeId + " BuildTime: " + buildTime);
            Assert.assertNotNull(buildTime);
        }
        packetUartIO.close();
    }


    @Test
    public void testSetPort() throws Exception {
        PacketUartIO packetUartIO = new PacketUartIO("COM1", 19200);

        int delay = 100;
        Packet resp;

        Node node = new Node(1, packetUartIO);

        log.debug("Sending setPortValue 1");
        resp = node.setPortValue('A', Bits.bit1 | Bits.bit0, 0, 0, 0xFF ^ Bits.bit1 ^ Bits.bit0);
        node.readMemory(Pic.PORTA);
        node.readMemory(Pic.TRISA);
        Assert.assertEquals(MessageType.MSG_SetPortA, resp.data[0]);
        Assert.assertEquals(0, resp.data[1] & 3);
        Assert.assertEquals(3, resp.data.length);
        Thread.sleep(delay);


        log.debug("Sending setPortValue 2");
        resp = node.setPortValue('A', 3, 1);
        Assert.assertEquals(MessageType.MSG_SetPortA, resp.data[0]);
        Assert.assertEquals(1, resp.data[1] & 3);
        Assert.assertEquals(2, resp.data.length);
        Thread.sleep(delay);

        log.debug("Sending setPortValue 3");
        resp = node.setPortValue('A', 3, 2);
        Assert.assertEquals(MessageType.MSG_SetPortA, resp.data[0]);
        Assert.assertEquals(2, resp.data[1] & 3);
        Assert.assertEquals(2, resp.data.length);
        Thread.sleep(delay);

        log.debug("Sending setPortValue 4");
        resp = node.setPortValue('A', 3, 3);
        Assert.assertEquals(MessageType.MSG_SetPortA, resp.data[0]);
        Assert.assertEquals(3, resp.data[1] & 3);
        Assert.assertEquals(2, resp.data.length);
        Thread.sleep(delay);

        int portCMask = 255 - Bits.bit7 - Bits.bit6;
        Node node2 = new Node(2, packetUartIO);
        log.debug("Sending setPortValue 5");
        resp = node2.setPortValue('C', portCMask, Bits.bit4 | Bits.bit5, 0, 0);
        Assert.assertEquals(MessageType.MSG_SetPortC, resp.data[0]);
        Assert.assertEquals((16 + 32), portCMask & resp.data[1]);
        Assert.assertEquals(3, resp.data.length);
        Thread.sleep(delay);

        log.debug("Sending setPortValue 6");
        resp = node2.setPortValue('C', Bits.bit5, 0);
        Assert.assertEquals(MessageType.MSG_SetPortC, resp.data[0]);
        Assert.assertEquals(16, resp.data[1] & portCMask);
        Assert.assertEquals(2, resp.data.length);
        Thread.sleep(delay);

        log.debug("Sending setPortValue 7");
        resp = node2.setPortValue('C', portCMask, Bits.bit5);
        Assert.assertEquals(MessageType.MSG_SetPortC, resp.data[0]);
        Assert.assertEquals(32, resp.data[1] & portCMask);
        Assert.assertEquals(2, resp.data.length);
        Thread.sleep(delay);

        log.debug("Sending setPortValue 8");
        resp = node2.setPortValue('C', portCMask, 0);
        Assert.assertEquals(MessageType.MSG_SetPortC, resp.data[0]);
        Assert.assertEquals(0, resp.data[1] & portCMask);
        Assert.assertEquals(2, resp.data.length);
        Thread.sleep(delay);


        packetUartIO.close();
    }

    @Test
    public void testButtons() throws Exception {
        PacketUartIO packetUartIO = new PacketUartIO("COM1", 19200);

        int delay = 100;

        Node node = new Node(1, packetUartIO);
        log.info(node.echo(1));
    }

    @Test
    public void testUartFreeze() throws Exception {
        PacketUartIO packetUartIO = new PacketUartIO("COM1", 19200);

        Node node = new Node(2, packetUartIO);
        node.setPortValue('C', 255, Bits.bit4 | Bits.bit5,
                0,
                Bits.bit0 | Bits.bit1 | Bits.bit2 | Bits.bit3);

        boolean res = true;
        for (int i = 0; ; i++) {
            log.info(i);
            //res = node.readMemory(node.Pic.PORTA);
//            res = node.echo(3);
//            Thread.sleep(10000);
            node.dumpMemory(new int[]{Pic.receiveQueue + 32, Pic.receiveQueue + 33,
                    Pic.receiveQueue + 34, Pic.sendPacketBuffer_dataLength_possition,
                    Pic.appFlags_uartReceiveBufferErrCount, Pic.appFlags_uartReceiveCrcErrCount,
                    Pic.PIE1, Pic.PIR1, Pic.INTCON, Pic.TRISC, Pic.PORTC,
                    Pic.receiveQueue + 13, Pic.receiveQueue + 14, Pic.receiveQueue + 15,
                    Pic.TXSTA, Pic.RCSTA, Pic.BAUDCON1,
            });
            node.setPortValue('C', Bits.bit5, ((i & 1) == 0) ? 0 : 255);
            node.setPortValue('C', Bits.bit4, ((i & 1) == 1) ? 0 : 255);
            log.info("***");
        }
//        Thread.sleep(10000);
    }

    @Test
    public void testDebugUart() throws Exception {
        PacketUartIO packetUartIO = new PacketUartIO("COM1", 19200);

        Node node = new Node(2, packetUartIO);
        boolean res = node.dumpMemory(new int[]{Pic.receiveQueue + 32, Pic.receiveQueue + 33,
                Pic.receiveQueue + 34, Pic.sendPacketBuffer_dataLength_possition,
                Pic.appFlags_uartReceiveBufferErrCount, Pic.appFlags_uartReceiveCrcErrCount,
                Pic.PIE1, Pic.PIR1, Pic.INTCON, Pic.TRISC, Pic.PORTC,
                Pic.receiveQueue + 13, Pic.receiveQueue + 14, Pic.receiveQueue + 15,
                Pic.TXSTA, Pic.RCSTA, Pic.BAUDCON1,
                Pic.portConfig_oldValueA, Pic.portConfig_oldValueB, Pic.portConfig_oldValueC, Pic.portConfig_oldValueD,
                Pic.portConfig_eventMaskA, Pic.portConfig_eventMaskB, Pic.portConfig_eventMaskC, Pic.portConfig_eventMaskD,
        });
        Assert.assertTrue(res);
    }

    @Test
    public void testButtonRead() throws Exception {
        PacketUartIO packetUartIO = new PacketUartIO("COM1", 19200);

        Node node1 = new Node(1, packetUartIO);
        Node node2 = new Node(2, packetUartIO);
        Packet resp;
        resp = node1.setPortValue('A', Bits.bit1 | Bits.bit0, 0, 0, 0xFF ^ Bits.bit1 ^ Bits.bit0);
        resp = node2.setPortValue('C', 255, Bits.bit4 | Bits.bit5,
                Bits.bit1 | Bits.bit2 | Bits.bit3,
                Bits.bit0 | Bits.bit1 | Bits.bit2 | Bits.bit3
        );
        while (true) {
            Thread.sleep(1000);
            boolean res = node2.dumpMemory(new int[]{Pic.heartBeatCounter});
        }
    }


    @Test
    public void testListen() throws Exception {
        PacketUartIO packetUartIO = new PacketUartIO("COM1", 19200);

        while (true) {
            Thread.sleep(2000);
        }
    }

    @Test
    public void testTmp() throws Exception {
        PacketUartIO packetUartIO = new PacketUartIO("COM1", 19200);

        final Node node1 = new Node(1, packetUartIO);
        final Node node2 = new Node(2, packetUartIO);
        //node2.setHeartBeatPeriod(1);

        System.out.println("id: " + node1.getNodeId());
        System.out.println(node1.getBuildTime());
//        System.out.println("id: " + node2.getNodeId());
//        System.out.println(node2.getBuildTime());
//        System.out.println("id: " + node2.getNodeId());
//        System.out.println(node2.getBuildTime());
    while (true) {
        node2.dumpMemory(new int[]{Pic.canReceiveLongMsgCount, Pic.canReceiveMismatch, Pic.COMSTAT, Pic.CANSTAT, Pic.CANCON, Pic.ECANCON,
                Pic.RXB0CON, Pic.RXB1CON, Pic.B0CON, Pic.B1CON, Pic.B2CON, Pic.B3CON, Pic.B4CON, Pic.B5CON,
                Pic.RXB0DLC, Pic.RXB1DLC,Pic.B0DLC, Pic.B1DLC,
                Pic.RXB0D0, Pic.RXB0D1, Pic.RXB0D2, Pic.RXB0D3, Pic.RXB0D4, Pic.RXB1D0, Pic.B0D0, Pic.B0D1, Pic.B0D2, Pic.B0D3, Pic.B0D4, Pic.B1D0, Pic.B2D0, Pic.B3D0, Pic.PIR5
        });
        System.out.println("=====================\n");
        Thread.sleep(2000);
    }

//        for (int i = 0; i < 255; i++) {
//            System.out.println(i);
//            node1.setPortValue('B', Bits.bit7, (i & 1) != 0 ? 255 : 0, 0, 0xFF ^ Bits.bit0 ^ Bits.bit7);
//            node1.writeMemory(Pic.displayValue, 0xFF, i);
//            Thread.sleep(200);
//        }
    }

    @Test
    public void testButtonEvent() throws Exception {
        PacketUartIO packetUartIO = new PacketUartIO("COM1", 19200);

        final Node node1 = new Node(1, packetUartIO);
        final Node node2 = new Node(2, packetUartIO);

        node1.addListener(new Node.Listener() {
            @Override
            public void onButtonDown(Node node, int pin) {
            }

            @Override
            public void onButtonUp(Node node, int pin, int downTime) {
            }

            @Override
            public void onReboot(Node node, int pingCounter, int rconValue) throws IOException, InvalidArgumentException {
                node.setPortValue('B', Bits.bit7, 0x00, 0x00, 0xFF ^ Bits.bit7 ^ Bits.bit0);
                node.setHeartBeatPeriod(10);
            }
        });

        node2.addListener(new Node.Listener() {
            int pwmValue = 0;
            int step = 8;

            @Override
            public void onButtonDown(Node node, int pin) {
                try {
                    switch (pin) {
                        case Node.pinA0:
                            node1.setPortValueNoWait('B', Bits.bit7, 0x00);
                            break;
                        case Node.pinA2:
                            pwmValue -= step;
                            if (pwmValue < 0) pwmValue = 0;
                            node2.setPwmValue(pwmValue);
                            System.out.println("PWM: " + pwmValue);
                            break;
                        case Node.pinA3:
                            node1.setPortValueNoWait('B', Bits.bit7, 0xFF);
                            break;
                        case Node.pinA5:
                            pwmValue += step;
                            if (pwmValue > 63) pwmValue = 63;
                            node2.setPwmValue(pwmValue);
                            System.out.println("PWM: " + pwmValue);
                            break;
                        case Node.pinB0: //SW3 button1
                            node2.setPortValue('C', Bits.bit6, 0x00);
                            break;
                        case Node.pinB1: //SW3 button2
                            node2.setPortValue('C', Bits.bit6, 0xFF);
                            break;
                        case Node.pinC5: //SW3 button3
                            node2.setPortValue('C', Bits.bit7, 0xFF);
                            break;
                        case Node.pinC4: //SW3 button4
                            node2.setPortValue('C', Bits.bit7, 0x00);
                            break;

                        case Node.pinC3: //SW2 button1
                            node2.setPortValue('C', Bits.bit2, 0x00);
                            break;
                        case Node.pinC1: //SW2 button2
                            node2.setPortValue('C', Bits.bit2, 0xFF);
                            break;
                        case Node.pinC0: //SW2 button3
                            node2.setPortValue('A', Bits.bit7, 0xFF);
                            break;
                        case Node.pinA6: //SW2 button4
                            node2.setPortValue('A', Bits.bit7, 0x00);
                            break;
                    }
                } catch (Exception e) {
                    log.error(e);
                }
            }

            @Override
            public void onButtonUp(Node node, int pin, int downTime) {
                try {
                    switch (pin) {
                        case Node.pinC1:
                            break;
                        case Node.pinC2:
                            break;
                        case Node.pinC3:
//                            node2.setPortValueNoWait('C', node.Bits.bit4, led3);
                            break;
                    }
                } catch (Exception e) {
                    log.error(e);
                }
            }

            @Override
            public void onReboot(Node node, int pingCounter, int rconValue) throws IOException, InvalidArgumentException {
                //To change body of implemented methods use File | Settings | File Templates.
                node.setPortValue('A', 0, 0,
                        Bits.bit0 | Bits.bit2 | Bits.bit3 | Bits.bit5 | Bits.bit6,
                        0xFF ^ Bits.bit7
                );
                node.setPortValue('B', 0, 0,
                        Bits.bit0 | Bits.bit1,
                        0xFF ^ Bits.bit4 ^ Bits.bit5
                );
                node.setPortValue('C', 0, 0,
                        Bits.bit0 | Bits.bit1 | Bits.bit3 | Bits.bit4 | Bits.bit5,
                        0xFF ^ Bits.bit2 ^ Bits.bit6 ^ Bits.bit7
                );
                //node.setPortValue('B', Bits.bit4 | Bits.bit5, 0, 0, 0xFF ^ Bits.bit4 ^ Bits.bit5);
                node.setHeartBeatPeriod(1);

                //todo: 62.5 khz. CPU frequency 4 instead of 16 MHz!!!
                //node.enablePwm(16, 3, 0); //62.5 khz
                //node2.setPwmValue(pwmValue);
            }
        });
        while (true) {
            Thread.sleep(1000);
            //node2.setPortValue('C', Bits.bit6, 0xFF);
            Thread.sleep(1000);
            //node2.setPortValue('C', Bits.bit6, 0x00);
//            node2.dumpMemory(new int[]{node.Pic.CCPR1L});
//            node1.dumpMemory(new int[]{node.Pic.PORTB, node.Pic.TRISB});
        }
    }

    @Test
    public void testPwm() throws Exception {
        PacketUartIO packetUartIO = new PacketUartIO("COM1", 19200);

        final Node node1 = new Node(1, packetUartIO);
        final Node node2 = new Node(2, packetUartIO);

        log.debug("node.Node #" + node1.getNodeId() + " build time: " + node1.getBuildTime());
        log.debug("node.Node #" + node2.getNodeId() + " build time: " + node2.getBuildTime());

        Packet resp;
        resp = node1.setPortValue('A', Bits.bit1 | Bits.bit0, Bits.bit0, 0, 0xFF ^ Bits.bit1 ^ Bits.bit0);
        resp = node2.setPortValue('C', Bits.bit2, 0xFF, 0, 0xFF ^ Bits.bit2);

        Thread.sleep(1000);
        resp = node1.setPortValue('A', Bits.bit1 | Bits.bit0, Bits.bit1);
        resp = node2.setPortValue('C', Bits.bit2, 0xFF);
        Thread.sleep(1000);
        resp = node1.setPortValue('A', Bits.bit1 | Bits.bit0, Bits.bit0);
        resp = node2.setPortValue('C', Bits.bit2, 0x00);
        node2.enablePwm(16, 3, 30);

        for (int i = 0; i < 64 * 3; i++) {
            node2.setPwmValue(i & 63);
            Thread.sleep(10);
        }

        node2.dumpMemory(new int[]{Pic.CCP1CON, Pic.CCPR1L, Pic.CCPR1H, Pic.BRGCON1, Pic.PR2, Pic.T2CON, Pic.TMR2, Pic.TMR2, Pic.TMR2, Pic.TMR2});
    }
}