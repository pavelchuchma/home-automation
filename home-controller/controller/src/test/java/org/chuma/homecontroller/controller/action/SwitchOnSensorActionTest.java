package org.chuma.homecontroller.controller.action;

import junit.framework.TestCase;
import org.junit.Assert;

import org.chuma.homecontroller.controller.actor.VoidOnOffActor;

public class SwitchOnSensorActionTest extends TestCase {
    public void testBasic() throws InterruptedException {
        VoidOnOffActor act1 = new VoidOnOffActor("act1");
        SwitchOnSensorAction action = new SwitchOnSensorAction(act1, 1);
        Assert.assertFalse(act1.isOn());

        action.perform(0);
        // wait a moment because switch on is async
        Thread.sleep(100);
        Assert.assertTrue(act1.isOn());
        Thread.sleep(800);
        Assert.assertTrue("Should be ON after 900ms", act1.isOn());

        Thread.sleep(200);
        Assert.assertFalse("Should be OFF after 1100ms", act1.isOn());

        action.perform(0);
        // wait a moment because switch on is async
        Thread.sleep(100);
        Assert.assertTrue(act1.isOn());
        Thread.sleep(800);
        Assert.assertTrue("Should be ON after 900ms", act1.isOn());

        // do action again to extend time
        action.perform(0);
        Thread.sleep(100);
        Assert.assertTrue(act1.isOn());
        Thread.sleep(800);
        Assert.assertTrue("Should be ON after 900ms", act1.isOn());

        Thread.sleep(200);
        Assert.assertFalse("Should be OFF after 1100ms", act1.isOn());
    }

    public void testParallelModification() throws InterruptedException {
        VoidOnOffActor act1 = new VoidOnOffActor("act1");
        SwitchOnSensorAction action = new SwitchOnSensorAction(act1, 1);
        Assert.assertFalse(act1.isOn());

        action.perform(0);
        // wait a moment because switch on is async
        Thread.sleep(100);
        Assert.assertTrue(act1.isOn());
        Thread.sleep(800);
        Assert.assertTrue("Should be ON after 900ms", act1.isOn());

        act1.switchOn(new Object());

        Thread.sleep(200);
        Assert.assertTrue("Should be ON after 1100ms because it was switched on by other call", act1.isOn());
    }
}