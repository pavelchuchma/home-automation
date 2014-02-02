package nodeImpl;

import app.NodeInfoCollector;
import node.Node;
import node.Pin;

import java.io.IOException;

public class GenericNodeListener extends AbstractNodeListener {


    public GenericNodeListener(NodeInfoCollector collector) {
        super(collector);
    }

    @Override
    public void onButtonDown(Node node, Pin pin) {
    }

    @Override
    public void onButtonUp(Node node, Pin pin, int downTime) {
    }

    @Override
    public void onReboot(Node node, int pingCounter, int rconValue) throws IOException, IllegalArgumentException {

    }
}