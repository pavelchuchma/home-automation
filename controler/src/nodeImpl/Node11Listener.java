package nodeImpl;

import app.NodeInfoCollector;
import com.sun.javaws.exceptions.InvalidArgumentException;
import node.Bits;
import node.Node;

import java.io.IOException;

public class Node11Listener extends AbstractNodeListener {

    int a1 = 0;
    int a2 = 0;
    int a3 = 0;
    int a4 = 0;

    public Node11Listener(NodeInfoCollector collector) {
        super(collector);
    }

    public void initNode(Node node) throws IOException, InvalidArgumentException {
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
    public void onButtonDown(Node node, int pin) {
        try {
            Node node3 = collector.getNode(3);
            if (node3 != null) {
                switch (pin) {
                    case Node.pinB0:
                        node3.setPortValue('C', Bits.bit3, a1);
                        node.setPortValue('C', Bits.bit6, a1);
                        a1 = 0xFF ^ a1;
                        break;
                    case Node.pinB1:
                        node3.setPortValue('C', Bits.bit1, a2);
                        a2 = 0xFF ^ a2;
                        node.setPortValue('C', Bits.bit7, a2);
                        break;
                    case Node.pinC5:
//                        node3.setPortValue('C', Bits.bit2, a3);
                        node3.setPortValue('C', Bits.bit0, a3);
                        a3 = 0xFF ^ a3;
                        break;
                    case Node.pinC4:
//                        node3.setPortValue('A', Bits.bit7, a4);
                        node3.setPortValue('A', Bits.bit6, a4);
                        a4 = 0xFF ^ a4;
                        break;
                }
            }
        } catch (InvalidArgumentException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    public void onButtonUp(Node node, int pin, int downTime) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onReboot(Node node, int pingCounter, int rconValue) throws IOException, InvalidArgumentException {
        initNode(node);
    }
}