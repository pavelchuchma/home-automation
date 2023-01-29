package org.chuma.homecontroller.controller.actor;

import junit.framework.TestCase;
import org.junit.Assert;

public class VoidOnOffActorTest extends TestCase {
    Boolean listenerState = null;

    public void testBasicPositive() {
        final VoidOnOffActor t1 = new VoidOnOffActor("t1", true, new ActorListener() {
            @Override
            public void onAction(IReadableOnOff source, Object actionData) {
                listenerState = source.isOn();
            }

            @Override
            public void addSource(IReadableOnOff source) {

            }
        });

        Assert.assertNull(listenerState);
        Assert.assertEquals(0, t1.getValue(), .001);
        t1.switchOn(null);
        Assert.assertTrue(listenerState);
        Assert.assertEquals(1, t1.getValue(), .001);
        t1.switchOff(null);
        Assert.assertFalse(listenerState);
        Assert.assertEquals(0, t1.getValue(), .001);
    }

    public void testBasicNegative() {
        final VoidOnOffActor t1 = new VoidOnOffActor("t1", false, new ActorListener() {
            @Override
            public void onAction(IReadableOnOff source, Object actionData) {
                listenerState = source.isOn();
            }

            @Override
            public void addSource(IReadableOnOff source) {

            }
        });

        Assert.assertNull(listenerState);
        Assert.assertEquals(1, t1.getValue(), .001);
        t1.switchOn(null);
        Assert.assertTrue(listenerState);
        Assert.assertEquals(0, t1.getValue(), .001);
        t1.switchOff(null);
        Assert.assertFalse(listenerState);
        Assert.assertEquals(1, t1.getValue(), .001);
    }
}