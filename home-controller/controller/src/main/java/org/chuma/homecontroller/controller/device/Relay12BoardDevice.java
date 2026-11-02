package org.chuma.homecontroller.controller.device;

import org.chuma.homecontroller.base.node.Node;

/**
 * 12 relays on two boards: connector 1 drives relays 1-6, connector 2 relays 12-7.
 * Relays 1-4 are on a 4-relay board triggered by high level, relays 5-12 on an 8-relay board triggered by low level.
 */
public class Relay12BoardDevice extends AbstractRelayBoardDevice {
    public Relay12BoardDevice(String id, Node node) {
        super(12);
        GenericOutputDevice port1 = new GenericOutputDevice(id + "1", node, 1, generateNames(id, new String[]{"1", "2", "3", "4", "5", "6"}), 0b00_1111);
        setRelays(1, port1.getOut4(), port1.getOut3(), port1.getOut2(), port1.getOut1(), port1.getOut5(), port1.getOut6());
        GenericOutputDevice port2 = new GenericOutputDevice(id + "2", node, 2, generateNames(id, new String[]{"12", "11", "10", "9", "8", "7"}), false);
        setRelays(7, port2.getOut4(), port2.getOut5(), port2.getOut6(), port2.getOut3(), port2.getOut2(), port2.getOut1());
    }
}
