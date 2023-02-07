package org.chuma.homecontroller.controller.device;

import java.io.IOException;

import junit.framework.TestCase;
import org.junit.Assert;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.Pin;
import org.chuma.homecontroller.base.packet.simulation.SimulatedNode;
import org.chuma.homecontroller.base.packet.simulation.SimulatedPacketUartIO;
import org.chuma.homecontroller.controller.actor.LddActor;

@SuppressWarnings("PointlessArithmeticExpression")
public class LddBoardDeviceTest extends TestCase {
    public void testSwitching() throws IOException {
        try (SimulatedPacketUartIO sim = new SimulatedPacketUartIO()) {
            sim.start();
            Node n = new Node(-1, "testA", sim);
            sim.registerNode(n);
            LddBoardDevice d = new LddBoardDevice("test", n, 1,
                    1.0, 2.0, 0.5, 1.0, 1.0, 1.0);

            LddActor ldd11 = new LddActor("ldd11", "", d.getLdd1(), .7);
            final int maxPwm11 = (int)(0.7 / 1.0 * 48.0);
            assertEquals(maxPwm11, ldd11.getLddOutput().getMaxPwmValue());


            Assert.assertEquals("setMaxOutputCurrent() can be called only once",
                    Assert.assertThrows(IllegalArgumentException.class,
                            () -> new LddActor("ldd11", "", d.getLdd1(), 1)).getMessage());

            Assert.assertTrue(Assert.assertThrows(IllegalArgumentException.class,
                            () -> new LddActor("ldd11", "", d.getLdd6(), 1.1)).getMessage()
                    .endsWith("is not enough for 1.10 A"));


            LddActor ldd12 = new LddActor("ldd11", "", d.getLdd2(), 1.5);
            final int maxPwm12 = (int)(1.5 / 2.0 * 48.0);
            assertEquals(maxPwm12, ldd12.getLddOutput().getMaxPwmValue());

            SimulatedNode sn = sim.getSimulatedNode(n.getNodeId());
            n.initialize();

            ExpectedPortsState exp = new ExpectedPortsState(sn);
            exp.setTrisZeroes(Pin.pinA5, Pin.pinA3, Pin.pinA2, Pin.pinA0, Pin.pinB4, Pin.pinB5);
            exp.assetState();

            Assert.assertEquals("Use switchOn(double value, Object actionData)",
                    Assert.assertThrows(IllegalStateException.class, () -> ldd11.switchOn(null)).getMessage());

            Assert.assertEquals("The value 5.0 is not in the specified inclusive range of 0.0 to 1.0",
                    Assert.assertThrows(IllegalArgumentException.class, () -> ldd11.switchOn(5, null)).getMessage());

            ldd11.switchOn(0.5, null);
            Assert.assertEquals((int)(maxPwm11 * 0.5), sn.getManualPwm(Pin.pinB5));

            ldd11.switchOff();
            Assert.assertEquals(0, sn.getManualPwm(Pin.pinB5));

            ldd11.switchOn(1.0, null);
            Assert.assertEquals((int)(maxPwm11 * 1.0), sn.getManualPwm(Pin.pinB5));
        }
    }
}