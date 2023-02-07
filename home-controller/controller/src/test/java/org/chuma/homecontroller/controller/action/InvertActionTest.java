package org.chuma.homecontroller.controller.action;

import junit.framework.TestCase;
import org.junit.Assert;

import org.chuma.homecontroller.controller.actor.VoidOnOffActor;

public class InvertActionTest extends TestCase {
    public void testInvertAction() {
        VoidOnOffActor act1 = new VoidOnOffActor("act1");
        InvertAction invertAction = new InvertAction(act1);
        Assert.assertFalse(act1.isOn());
        invertAction.perform(0);
        Assert.assertTrue(act1.isOn());
        invertAction.perform(0);
        Assert.assertFalse(act1.isOn());
    }
}