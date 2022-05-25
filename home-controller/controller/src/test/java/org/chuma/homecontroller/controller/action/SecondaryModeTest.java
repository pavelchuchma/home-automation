package org.chuma.homecontroller.controller.action;

import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import org.junit.Assert;

import org.chuma.homecontroller.controller.actor.ActorListener;
import org.chuma.homecontroller.controller.actor.IReadableOnOff;

public class SecondaryModeTest extends TestCase {
    private static final int TIMEOUT_MS = 200;
    Boolean listenerState;

    public void testSimple() throws InterruptedException {
        listenerState = null;
        SecondaryMode sm = new SecondaryMode(TIMEOUT_MS, new ActorListener() {
            @Override
            public void onAction(IReadableOnOff source, Object actionData) {
                listenerState = source.isOn();
            }

            @Override
            public void addSource(IReadableOnOff source) {

            }
        });

        Assert.assertNull(listenerState);

        // switch on
        sm.switchState();
        Assert.assertTrue(listenerState);

        // switch off
        sm.switchState();
        Assert.assertFalse(listenerState);

        // switch on
        sm.switchState();
        Assert.assertTrue(listenerState);
        // test timeout
        TimeUnit.MILLISECONDS.sleep((long)(TIMEOUT_MS * 0.6));
        Assert.assertTrue(listenerState);
        TimeUnit.MILLISECONDS.sleep((long)(TIMEOUT_MS * 0.6));
        Assert.assertFalse(listenerState);

        // test timeout + prolong
        // switch on
        sm.switchState();
        Assert.assertTrue(listenerState);
        TimeUnit.MILLISECONDS.sleep((long)(TIMEOUT_MS * 0.6));
        Assert.assertTrue(listenerState);
        // touch it, should be on
        Assert.assertTrue(sm.isActiveAndTouch());
        TimeUnit.MILLISECONDS.sleep((long)(TIMEOUT_MS * 0.6));
        Assert.assertTrue(listenerState);
        // timed out, should be off
        TimeUnit.MILLISECONDS.sleep((long)(TIMEOUT_MS * 0.6));
        Assert.assertFalse(sm.isActiveAndTouch());
        Assert.assertFalse(listenerState);
    }
}