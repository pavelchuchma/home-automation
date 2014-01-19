package nodeImpl;

import app.NodeInfoCollector;
import com.sun.javaws.exceptions.InvalidArgumentException;
import node.Bits;
import node.Node;

import java.io.IOException;

public class Node03Listener extends AbstractNodeListener {


    public Node03Listener(NodeInfoCollector collector) {
        super(collector);
    }

    public void initNode(Node node) throws IOException, InvalidArgumentException {
//        node.setHeartBeatPeriod(2);
        //node.setFrequency(2, 3); //  2 MHz
//        node.setFrequency(4, 7); //  4 MHz
        //node.setFrequency(16); //  8 MHz

        // set SW2 as outputs with 0
        node.setPortValue('A', Bits.bit6 | Bits.bit7, 0xFF, 0x00, 0xFF ^ Bits.bit6 ^ Bits.bit7);
        node.setPortValue('C', Bits.bit0 | Bits.bit1 | Bits.bit2 | Bits.bit3, 0xFF, 0x00, 0xFF ^ Bits.bit0 ^ Bits.bit1 ^ Bits.bit2 ^ Bits.bit3);
    }

    @Override
    public void onButtonDown(Node node, int pin) {
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