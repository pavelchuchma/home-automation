package nodeImpl;

import app.NodeInfoCollector;
import node.Bits;
import node.Node;
import node.Pin;

import java.io.IOException;

public class Node03Listener extends AbstractNodeListener {


    public void initNode(Node node) throws IOException, IllegalArgumentException {
//        node.setHeartBeatPeriod(2);
        //node.setFrequency(2, 3); //  2 MHz
//        node.setFrequency(4, 7); //  4 MHz
        //node.setFrequency(16); //  8 MHz

        // set SW2 as outputs with 0
        node.setPortValue('A', Bits.bit6 | Bits.bit7, 0xFF, 0x00, 0xFF ^ Bits.bit6 ^ Bits.bit7);
        node.setPortValue('C', Bits.bit0 | Bits.bit1 | Bits.bit2 | Bits.bit3, 0xFF, 0x00, 0xFF ^ Bits.bit0 ^ Bits.bit1 ^ Bits.bit2 ^ Bits.bit3);
    }

    @Override
    public void onButtonDown(Node node, Pin pin) {
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