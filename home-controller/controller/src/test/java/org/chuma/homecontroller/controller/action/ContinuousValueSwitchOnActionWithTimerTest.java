package org.chuma.homecontroller.controller.action;

import java.io.IOException;

import junit.framework.TestCase;
import org.junit.Assert;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.Pin;
import org.chuma.homecontroller.base.node.PwmOutputNodePin;
import org.chuma.homecontroller.base.packet.simulation.SimulatedPacketUartIO;
import org.chuma.homecontroller.controller.actor.PwmActor;

public class ContinuousValueSwitchOnActionWithTimerTest extends TestCase {
    public void testBasic() throws InterruptedException, IOException {
        try (SimulatedPacketUartIO packetUartIO = new SimulatedPacketUartIO()) {
            packetUartIO.start();
            Node n = new Node(-1, "testA", packetUartIO);
            PwmActor pwmActor1 = new PwmActor("p1", "p1", new PwmOutputNodePin("1", "pwm1", n, Pin.pinA0, 20));

            ContinuousValueSwitchOnActionWithTimer action = new ContinuousValueSwitchOnActionWithTimer(pwmActor1, 1, 0.5);
            Assert.assertFalse(pwmActor1.isOn());

            action.perform(0);
            // wait a moment because switch on is async
            Thread.sleep(100);
            Assert.assertEquals(10, pwmActor1.getCurrentPwmValue());
            Thread.sleep(800);
            Assert.assertTrue("Should be ON after 900ms", pwmActor1.isOn());

            Thread.sleep(200);
            Assert.assertFalse("Should be OFF after 1100ms", pwmActor1.isOn());
        }
    }
}