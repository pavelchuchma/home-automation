package org.chuma.homecontroller.controller.actor;

import junit.framework.TestCase;
import org.junit.Assert;

public class VoidOnOffActorTest extends TestCase {
    Boolean listenerState = null;

    public void testBasicPositive() {
        final VoidOnOffActor t1 = new VoidOnOffActor("t1", new ActorListener() {
            @Override
            public void onAction(IReadableOnOff source, Object actionData) {
                listenerState = source.isOn();
            }

            @Override
            public void addSource(IReadableOnOff source) {

            }
        });

        Assert.assertNull(listenerState);
        Assert.assertFalse(t1.isOn());
        t1.switchOn(null);
        Assert.assertTrue(listenerState);
        Assert.assertTrue(t1.isOn());
        t1.switchOff(null);
        Assert.assertFalse(listenerState);
        Assert.assertFalse(t1.isOn());
    }
}