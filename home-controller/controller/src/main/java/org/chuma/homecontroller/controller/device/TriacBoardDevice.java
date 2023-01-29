package org.chuma.homecontroller.controller.device;

import org.chuma.homecontroller.base.node.Node;

public class TriacBoardDevice extends GenericOutputDevice {
    public TriacBoardDevice(String id, Node node, int connectorPosition) {
        super(id, node, connectorPosition, false);
    }
}
