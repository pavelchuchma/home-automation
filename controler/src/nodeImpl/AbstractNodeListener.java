package nodeImpl;

import app.NodeInfoCollector;
import node.Node;

public abstract class AbstractNodeListener  implements Node.Listener{
    NodeInfoCollector collector;

    protected AbstractNodeListener(NodeInfoCollector collector) {
        this.collector = collector;
    }
}