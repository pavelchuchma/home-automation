package nodeImpl;

import app.NodeInfoCollector;
import node.Bits;
import node.Node;
import node.Pin;
import org.apache.log4j.Logger;

import java.io.IOException;

public class Node11Listener extends AbstractNodeListener {
    static Logger log = Logger.getLogger(Node11Listener.class.getName());

    int a1 = 0;
    int a2 = 0;
    int a3 = 0;
    int a4 = 0;

    public Node11Listener() {
    }

    public void initNode(Node node) throws IOException, IllegalArgumentException {
//        node.setHeartBeatPeriod(2);
        //node.setFrequency(2, 3); //  2 MHz
//        node.setFrequency(4, 7); //  4 MHz
        //node.setFrequency(16); //  8 MHz

        // set switch on SW3
        /*
            BTN1    21 - RB0/AN10/C1INA/FLT0/INT0
            BTN2    22 - RB1/AN8/C1INB/P1B/CTDIN/INT1
            BTN3    16 - RC5/SDO
            BTN4    15 - RC4/SDA/SDI
            GREEN   17 - RC6/CANTX/TX1/CK1/CCP3
            RED     18 - RC7/CANRX/RX1/DT1/CCP4

         */
        node.setPortValue('B', 0, 0, Bits.bit0 | Bits.bit1, 0xFF);
        node.setPortValue('C', Bits.bit6 | Bits.bit7, 0xFF ^ Bits.bit7, Bits.bit4 | Bits.bit5, 0xFF ^ Bits.bit6 ^ Bits.bit7);
    }

    @Override
    public void onButtonDown(Node node, Pin pin) {
        /*
        try {
            Node node3 = NodeInfoCollector.getInstance().getNode(3);
            if (node3 != null) {
                switch (pin) {
                    case pinB0:
                        for (int i=0; i<5; i++) {
                            if (node3.setPinValue(Pin.pinC3, a1) != null) break;
                        }
                        node.setPinValue(Pin.pinC6, a1);
                        a1 = 0xFF ^ a1;
                        break;
                    case pinB1:
                        for (int i=0; i<5; i++) {
                            if (node3.setPinValue(Pin.pinC1, a2) != null) break;
                        }
                        a2 = 0xFF ^ a2;
                        node.setPinValue(Pin.pinC7, a2);
                        break;
                    case pinC5:
                        for (int i=0; i<5; i++) {
                            if (node3.setPinValue(Pin.pinC0, a3) != null) break;
                        }
                        a3 = 0xFF ^ a3;
                        break;
                    case pinC4:
                        for (int i=0; i<5; i++) {
                            if (node3.setPinValue(Pin.pinA6, a4) != null) break;
                        }
                        a4 = 0xFF ^ a4;
                        break;
                }
            }
        } catch (IllegalArgumentException e) {
            log.error("onButtonDown ERROR:", e);
        } catch (IOException e) {
            log.error("onButtonDown ERROR:", e);
        }
        */
    }

    @Override
    public void onButtonUp(Node node, Pin pin, int downTime) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onReboot(Node node, int pingCounter, int rconValue) throws IOException, IllegalArgumentException {
        initNode(node);
    }
}