package org.chuma.homecontroller.base.node;

import java.io.IOException;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.packet.Packet;
import org.chuma.homecontroller.base.packet.PacketUartIO;
import org.chuma.homecontroller.base.packet.PacketUartIOException;
import org.chuma.homecontroller.controller.ActionBinding;
import org.chuma.homecontroller.controller.action.Action;
import org.chuma.homecontroller.controller.device.GenericInputDevice;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoRegistry;
import org.chuma.homecontroller.controller.nodeinfo.NodeListener;

public class NodeTest {
    static Logger log = LoggerFactory.getLogger(NodeTest.class.getName());

    @Test
    public void testReadRam() throws Exception {
        PacketUartIO packetUartIO = new PacketUartIO("COM1", 19200);
        Node node = new Node(1, packetUartIO);

        log.debug("node.Node " + node.getNodeId() + " val: " + node.readMemory(0x43));
        packetUartIO.close();
    }

    @Test
    public void testGetBuildDate() throws Exception {
        PacketUartIO packetUartIO = new PacketUartIO("COM1", 19200);

        for (int i = 1; i <= 2; i++) {
            Node node = new Node(i, packetUartIO);

            Date buildTime = node.getBuildTime();
            log.debug("node.Node " + node.getNodeId() + " BuildTime: " + buildTime);
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
        log.info(String.valueOf(node.echo(1)));
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
            log.info(String.valueOf(i));
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
                    Pic.RXB0DLC, Pic.RXB1DLC, Pic.B0DLC, Pic.B1DLC,
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
            public void onInputLow(Node node, Pin pin, int highDuration) {
            }

            @Override
            public void onInputHigh(Node node, Pin pin, int lowDuration) {
            }

            @Override
            public void onReboot(Node node, int pingCounter, int rconValue) throws IOException {
                node.setPortValue('B', Bits.bit7, 0x00, 0x00, 0xFF ^ Bits.bit7 ^ Bits.bit0);
                node.setHeartBeatPeriod(10);
            }

            @Override
            public void onInitialized(Node node) {
            }
        });

        node2.addListener(new Node.Listener() {
            int pwmValue = 0;
            int step = 8;

            @Override
            public void onInputLow(Node node, Pin pin, int highDuration) {
                try {
                    switch (pin) {
                        case pinA0:
                            node1.setPortValueNoWait('B', Bits.bit7, 0x00);
                            break;
                        case pinA2:
                            pwmValue -= step;
                            if (pwmValue < 0) pwmValue = 0;
                            node2.setPwmValue(pwmValue);
                            System.out.println("PWM: " + pwmValue);
                            break;
                        case pinA3:
                            node1.setPortValueNoWait('B', Bits.bit7, 0xFF);
                            break;
                        case pinA5:
                            pwmValue += step;
                            if (pwmValue > 63) pwmValue = 63;
                            node2.setPwmValue(pwmValue);
                            System.out.println("PWM: " + pwmValue);
                            break;
                        case pinB0: //SW3 button1
                            node2.setPortValue('C', Bits.bit6, 0x00);
                            break;
                        case pinB1: //SW3 button2
                            node2.setPortValue('C', Bits.bit6, 0xFF);
                            break;
                        case pinC5: //SW3 button3
                            node2.setPortValue('C', Bits.bit7, 0xFF);
                            break;
                        case pinC4: //SW3 button4
                            node2.setPortValue('C', Bits.bit7, 0x00);
                            break;

                        case pinC3: //SW2 button1
                            node2.setPortValue('C', Bits.bit2, 0x00);
                            break;
                        case pinC1: //SW2 button2
                            node2.setPortValue('C', Bits.bit2, 0xFF);
                            break;
                        case pinC0: //SW2 button3
                            node2.setPortValue('A', Bits.bit7, 0xFF);
                            break;
                        case pinA6: //SW2 button4
                            node2.setPortValue('A', Bits.bit7, 0x00);
                            break;
                    }
                } catch (Exception e) {
                    log.error("err", e);
                }
            }

            @Override
            public void onInputHigh(Node node, Pin pin, int lowDuration) {
                try {
                    switch (pin) {
                        case pinC1:
                            break;
                        case pinC2:
                            break;
                        case pinC3:
//                            node2.setPortValueNoWait('C', node.Bits.bit4, led3);
                            break;
                    }
                } catch (Exception e) {
                    log.error("err", e);
                }
            }

            @Override
            public void onReboot(Node node, int pingCounter, int rconValue) throws IOException {
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

            @Override
            public void onInitialized(Node node) {
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


    @Test
    public void testManualPwm() throws Exception {
        PacketUartIO packetUartIO = new PacketUartIO("COM1", 19200);

        final Node node1 = new Node(1, packetUartIO);
        final Node node2 = new Node(2, packetUartIO);

        node1.addListener(new Node.Listener() {
            @Override
            public void onInputLow(Node node, Pin pin, int highDuration) {
            }

            @Override
            public void onInputHigh(Node node, Pin pin, int lowDuration) {
            }

            @Override
            public void onReboot(Node node, int pingCounter, int rconValue) throws IOException {
                node.setPortValue('B', Bits.bit7, 0x00, 0x00, 0xFF ^ Bits.bit7 ^ Bits.bit0);
                node.setHeartBeatPeriod(10);
            }

            @Override
            public void onInitialized(Node node) {
            }
        });

        node2.addListener(new Node.Listener() {
            int pwmValue = 0;
            int step = 8;

            @Override
            public void onInputLow(Node node, Pin pin, int highDuration) {
                try {
                    switch (pin) {
                        case pinA0:
                            node1.setPortValueNoWait('B', Bits.bit7, 0x00);
                            break;
                        case pinA2:
                            pwmValue -= step;
                            if (pwmValue < 0) pwmValue = 0;
                            node2.setPwmValue(pwmValue);
                            System.out.println("PWM: " + pwmValue);
                            break;
                        case pinA3:
                            node1.setPortValueNoWait('B', Bits.bit7, 0xFF);
                            break;
                        case pinA5:
                            pwmValue += step;
                            if (pwmValue > 63) pwmValue = 63;
                            node2.setPwmValue(pwmValue);
                            System.out.println("PWM: " + pwmValue);
                            break;
                        case pinB0: //SW3 button1
                            node2.setPortValue('C', Bits.bit6, 0x00);
                            break;
                        case pinB1: //SW3 button2
                            node2.setPortValue('C', Bits.bit6, 0xFF);
                            break;
                        case pinC5: //SW3 button3
                            node2.setPortValue('C', Bits.bit7, 0xFF);
                            break;
                        case pinC4: //SW3 button4
                            node2.setPortValue('C', Bits.bit7, 0x00);
                            break;

                        case pinC3: //SW2 button1
                            node2.setPortValue('C', Bits.bit2, 0x00);
                            break;
                        case pinC1: //SW2 button2
                            node2.setPortValue('C', Bits.bit2, 0xFF);
                            break;
                        case pinC0: //SW2 button3
                            node2.setPortValue('A', Bits.bit7, 0xFF);
                            break;
                        case pinA6: //SW2 button4
                            node2.setPortValue('A', Bits.bit7, 0x00);
                            break;
                    }
                } catch (Exception e) {
                    log.error("err", e);
                }
            }

            @Override
            public void onInputHigh(Node node, Pin pin, int lowDuration) {
                try {
                    switch (pin) {
                        case pinC1:
                            break;
                        case pinC2:
                            break;
                        case pinC3:
//                            node2.setPortValueNoWait('C', node.Bits.bit4, led3);
                            break;
                    }
                } catch (Exception e) {
                    log.error("err", e);
                }
            }

            @Override
            public void onReboot(Node node, int pingCounter, int rconValue) throws IOException {
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

            @Override
            public void onInitialized(Node node) {
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
    public void testCheckFrequency() throws Exception {
        PacketUartIO packetUartIO = new PacketUartIO("COM1", 19200);

        Node node = new Node(3, packetUartIO);
        node.dumpMemory(new int[]{Pic.OSCCON, Pic.BRGCON1, Pic.T0CON});
        log.info("** SETING ***");
        node.setFrequency(CpuFrequency.twoMHz);
        //node.setFrequency(4);
        //node.setFrequency(8);
        //node.setFrequency(16);
        log.info("** DONE **");
        node.dumpMemory(new int[]{Pic.OSCCON, Pic.BRGCON1, Pic.T0CON});
        log.info("*****");
        Thread.sleep(20000);
    }


    @Test
    public void testPinGetters() {
        Assert.assertEquals('A', Pin.pinA0.getPort());
        Assert.assertEquals('B', Pin.pinB7.getPort());
        Assert.assertEquals('C', Pin.pinC3.getPort());
        Assert.assertEquals('D', Pin.pinD7.getPort());
        Assert.assertEquals(1, Pin.pinA0.getBitMask());
        Assert.assertEquals(1, Pin.pinC0.getBitMask());
        Assert.assertEquals(128, Pin.pinC7.getBitMask());
    }

    @Test
    public void testSetPinValue() throws Exception {
        PacketUartIO packetUartIO = new PacketUartIO("COM1", 19200);

        int a = 1;
        switch (a) {
            case 0:
                break;
            case 1:
                for (int x = 0; x < 10; x++) {
                    if (x > 1) break;
                    System.out.print("x:" + x);
                }
                System.out.print("contine");
                break;
            default:
        }


        Node node = new Node(3, packetUartIO);
        node.setPinValue(Pin.pinB2, 1);
    }

    @Test
    public void testInitialization() throws PacketUartIOException {
        String port = (System.getenv("COMPUTERNAME") != null) ? "COM1" : "/dev/ttyS80";
        PacketUartIO packetUartIO = new PacketUartIO(port, 19200);
        NodeInfoRegistry nodeInfoRegistry = new NodeInfoRegistry(packetUartIO);

        NodeListener lst = nodeInfoRegistry.getNodeListener();

        Node pirNodeA = nodeInfoRegistry.createNode(7, "PirNodeA");
        GenericInputDevice pirA1Prizemi = new GenericInputDevice("PirA1Prizemi", pirNodeA, 3);
        lst.addActionBinding(new ActionBinding(pirA1Prizemi.getIn5AndActivate(), (Action) null, null));

        pirNodeA.initialize();

    }
}