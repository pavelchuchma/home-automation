package org.chuma.homecontroller.controller.action;

import java.io.IOException;

import junit.framework.TestCase;
import org.junit.Assert;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.Pin;
import org.chuma.homecontroller.base.node.PwmOutputNodePin;
import org.chuma.homecontroller.base.packet.simulation.SimulatedPacketUartIO;
import org.chuma.homecontroller.controller.actor.PwmActor;
import org.chuma.homecontroller.controller.actor.VoidOnOffActor;

public class SwitchAllOffWithMemoryTest extends TestCase {
    private static void assertValues(PwmActor pwmActor1, double expVal1, PwmActor pwmActor2, double expVal2,
                                     VoidOnOffActor voidActor3, boolean expVal3, VoidOnOffActor voidActor4, boolean expVal4) {
        Assert.assertEquals(pwmActor1.getValue(), expVal1, 0.001);
        Assert.assertEquals(pwmActor2.getValue(), expVal2, 0.001);
        Assert.assertEquals(voidActor3.isOn(), expVal3);
        Assert.assertEquals(voidActor4.isOn(), expVal4);
    }

    public void testBasic() throws IOException {
        try (SimulatedPacketUartIO packetUartIO = new SimulatedPacketUartIO()) {
            packetUartIO.start();
            Node n = new Node(-1, "testA", packetUartIO);
            PwmActor pwmActor1 = new PwmActor("p1", "p1", new PwmOutputNodePin("1", "pwm1", n, Pin.pinA0, 20));
            PwmActor pwmActor2 = new PwmActor("p2", "p2", new PwmOutputNodePin("2", "pwm2", n, Pin.pinA0, 20));
            VoidOnOffActor voidActor3 = new VoidOnOffActor("void3");
            VoidOnOffActor voidActor4 = new VoidOnOffActor("void4");

            SwitchAllOffWithMemory sw = new SwitchAllOffWithMemory(pwmActor1, pwmActor2, voidActor3, voidActor4);
            assertValues(pwmActor1, 0, pwmActor2, 0, voidActor3, false, voidActor4, false);
            Assert.assertThrows(IllegalArgumentException.class, () -> sw.perform(0));
            pwmActor2.setValue(.2, null);
            voidActor3.switchOn(null);
            assertValues(pwmActor1, 0, pwmActor2, 0.2, voidActor3, true, voidActor4, false);
            sw.perform(0);
            assertValues(pwmActor1, 0, pwmActor2, 0, voidActor3, false, voidActor4, false);
            sw.perform(0);
            assertValues(pwmActor1, 0, pwmActor2, 0.2, voidActor3, true, voidActor4, false);
            pwmActor1.setValue(0.7, null);
            assertValues(pwmActor1, 0.7, pwmActor2, 0.2, voidActor3, true, voidActor4, false);
            sw.perform(0);
            assertValues(pwmActor1, 0, pwmActor2, 0, voidActor3, false, voidActor4, false);
            voidActor4.switchOn(null);
            assertValues(pwmActor1, 0, pwmActor2, 0, voidActor3, false, voidActor4, true);
            sw.perform(0);
            assertValues(pwmActor1, 0, pwmActor2, 0, voidActor3, false, voidActor4, false);
            sw.perform(0);
            assertValues(pwmActor1, 0, pwmActor2, 0, voidActor3, false, voidActor4, true);
        }
    }
}