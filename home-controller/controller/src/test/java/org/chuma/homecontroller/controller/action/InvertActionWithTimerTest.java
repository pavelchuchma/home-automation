package org.chuma.homecontroller.controller.action;

import junit.framework.TestCase;
import org.junit.Assert;

import org.chuma.homecontroller.controller.actor.VoidOnOffActor;

public class InvertActionWithTimerTest extends TestCase {

    public void testInvertActionWithTimer() throws InterruptedException {
        VoidOnOffActor act1 = new VoidOnOffActor("act1");
        InvertActionWithTimer action = new InvertActionWithTimer(act1, 1);
        Assert.assertFalse(act1.isOn());

        action.perform(0);
        // wait a moment because switch on is async
        Thread.sleep(100);
        Assert.assertTrue(act1.isOn());

        action.perform(0);
        Assert.assertFalse(act1.isOn());

        action.perform(0);
        // wait a moment because switch on is async
        Thread.sleep(100);
        Assert.assertTrue(act1.isOn());
        Thread.sleep(1100);
        Assert.assertFalse(act1.isOn());
    }


}