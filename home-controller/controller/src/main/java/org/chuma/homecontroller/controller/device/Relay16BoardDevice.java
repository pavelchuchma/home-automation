package org.chuma.homecontroller.controller.device;

import org.chuma.homecontroller.base.node.Node;

/**
 * 16-relay board: connector 1 drives relays 10-5, connector 2 relays 16-11, connector 3 relays 1-4.
 */
public class Relay16BoardDevice extends AbstractRelayBoardDevice {
    public Relay16BoardDevice(String id, Node node) {
        this(id, node, true, true, true);
    }

    public Relay16BoardDevice(String id, Node node, boolean usePort1, boolean usePort2, boolean usePort3) {
        super(16);
        if (usePort1) {
            GenericOutputDevice port1 = new GenericOutputDevice(id + "1", node, 1, generateNames(id, new String[]{"10", "9", "8", "7", "6", "5"}), false);
            setRelays(5, port1.getOut6(), port1.getOut5(), port1.getOut4(), port1.getOut3(), port1.getOut2(), port1.getOut1());
        }
        if (usePort2) {
            GenericOutputDevice port2 = new GenericOutputDevice(id + "2", node, 2, generateNames(id, new String[]{"16", "15", "14", "13", "12", "11"}), false);
            setRelays(11, port2.getOut6(), port2.getOut5(), port2.getOut4(), port2.getOut3(), port2.getOut2(), port2.getOut1());
        }
        if (usePort3) {
            GenericOutputDevice port3 = new GenericOutputDevice(id + "3", node, 3, generateNames(id, new String[]{"1", "2", "3", "4", "NC", "NC"}), false);
            setRelays(1, port3.getOut1(), port3.getOut2(), port3.getOut3(), port3.getOut4());
        }
    }
}
