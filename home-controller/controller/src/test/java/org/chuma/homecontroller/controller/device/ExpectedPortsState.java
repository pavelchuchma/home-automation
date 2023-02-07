package org.chuma.homecontroller.controller.device;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import static org.chuma.homecontroller.base.packet.simulation.SimulatedPacketUartIO.PORT_ADDRESS;
import static org.chuma.homecontroller.base.packet.simulation.SimulatedPacketUartIO.TRIS_ADDRESS;

import org.chuma.homecontroller.base.node.Pin;
import org.chuma.homecontroller.base.packet.simulation.SimulatedNode;

class ExpectedPortsState {
    private final static long ONES = 0b11111111_11111111_11111111_11111111_11111111L;
    private final SimulatedNode node;
    long portState;
    long trisState;

    public ExpectedPortsState(SimulatedNode node, long portState, long trisState) {
        this.node = node;
        this.portState = portState;
        this.trisState = trisState;
    }

    public ExpectedPortsState(SimulatedNode node) {
        this(node, 0, ONES);
        setTrisZeroes(Pin.pinB2);
    }

    private static long buildMask(Pin[] pins) {
        long tmp = 0;
        for (Pin pin : pins) {
            tmp |= 1L << pin.ordinal();
        }
        return tmp;
    }

    void setPortOnes(Pin... pins) {
        portState |= buildMask(pins);
    }

    void setPortZeroes(Pin... pins) {
        portState &= ~buildMask(pins);
    }

    void setTrisOnes(Pin... pins) {
        trisState |= buildMask(pins);
    }

    void setTrisZeroes(Pin... pins) {
        trisState &= ~buildMask(pins);
    }

    private String toBinaryString(long l) {
        List<String> list = new ArrayList<>();
        for (int i = 4; i >= 0; i--) {
            list.add(String.format("%8s", Long.toBinaryString((l >> i * 8) & 0xFF)).replace(' ', '0'));
        }
        return String.join("_", list);
//            return String.format("%42s", Long.toBinaryString(l)).replace(' ', '0');
    }

    /**
     * @return TRIS A-E values as a single long. A as LSB.
     */
    private long readTrisValues() {
        return readMemoryValues(TRIS_ADDRESS);
    }

    /**
     * @return PORT A-E values as a single long. A as LSB.
     */
    public long readPortValues() {
        return readMemoryValues(PORT_ADDRESS);
    }

    private long readMemoryValues(int[] addresses) {
        long res = 0;
        for (int i = 0; i < addresses.length; i++) {
            res += (long)node.readRam(addresses[i]) << 8 * i;
        }
        return res;
    }

    void assetState() {
        final long realPortValues = readPortValues();
        Assert.assertEquals(String.format("Port exp: %s\n    real: %s\n",
                toBinaryString(portState), toBinaryString(realPortValues)), portState, realPortValues);
        final long realTrisValues = readTrisValues();
        Assert.assertEquals(String.format("Tris exp: %s\n    real: %s\n",
                toBinaryString(trisState), toBinaryString(realTrisValues)), trisState, realTrisValues);
    }

    void printDebug() {
        System.out.printf("Port exp: %s\n    real: %s\n",
                toBinaryString(portState), toBinaryString(readPortValues()));
        System.out.printf("Tris exp: %s\n    real: %s\n",
                toBinaryString(trisState), toBinaryString(readTrisValues()));
    }
}
