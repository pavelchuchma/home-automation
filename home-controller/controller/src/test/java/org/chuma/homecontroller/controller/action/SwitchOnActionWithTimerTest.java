package org.chuma.homecontroller.controller.action;

import junit.framework.TestCase;
import org.junit.Assert;

import org.chuma.homecontroller.controller.actor.VoidOnOffActor;

public class SwitchOnActionWithTimerTest extends TestCase {
    public void testBasic() throws InterruptedException {
        VoidOnOffActor act1 = new VoidOnOffActor("act1");
        SwitchOnActionWithTimer action = new SwitchOnActionWithTimer(act1, 1);
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
        SwitchOnActionWithTimer action = new SwitchOnActionWithTimer(act1, 1);
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

    public void testLowPriorityModification() throws InterruptedException {
        VoidOnOffActor act1 = new VoidOnOffActor("act1");
        SwitchOnActionWithTimer action = new SwitchOnActionWithTimer(act1, 1);
        Assert.assertFalse(act1.isOn());

        Object externalActionData = new Object();
        act1.switchOn(externalActionData);

        action.perform(0);
        // wait a moment because switch on is async
        Thread.sleep(100);
        Assert.assertTrue(act1.isOn());
        Assert.assertEquals(externalActionData, act1.getActionData());
        Thread.sleep(1100);
        Assert.assertTrue("Should be ON after timeout because it was switched ON by external action", act1.isOn());

        act1.switchOff();
    }

    public void testHighPriorityModification() throws InterruptedException {
        VoidOnOffActor act1 = new VoidOnOffActor("act1");
        SwitchOnActionWithTimer action = new SwitchOnActionWithTimer(act1, 1, AbstractSwitchOnActionWithTimer.Priority.HIGH, null);
        Assert.assertFalse(act1.isOn());

        Object externalActionData = new Object();
        act1.switchOn(externalActionData);

        action.perform(0);
        // wait a moment because switch on is async
        Thread.sleep(100);
        Assert.assertTrue(act1.isOn());
        Assert.assertTrue("action data should be overwritten", act1.getActionData() instanceof AbstractSwitchOnActionWithTimer.ActionData);
        Thread.sleep(1100);
        Assert.assertFalse("Should be OFF because it should be overwritten", act1.isOn());
    }
}