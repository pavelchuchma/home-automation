package org.chuma.homecontroller.controller.controller;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

public class ValveControllerImplTest extends AbstractControllerTest {
    final int valveSwitchDuration = 1_000;
    Actor openActor = new Actor("OPEN");
    Actor closeActor = new Actor("CLOSE");

    @Test
    public void testValveClose() {
        ValveController vc = new ValveControllerImpl("vc", "LC", openActor, closeActor, valveSwitchDuration);

        vc.close();

        Iterator<ActionItem> iterator = actions.iterator();
        Assert.assertEquals(new ActionItem(openActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(closeActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(closeActor, "off", valveSwitchDuration, 20), iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void testValveManipulation() {
        ValveController vc = new ValveControllerImpl("vc", "LC", openActor, closeActor, valveSwitchDuration);

        vc.close();
        vc.setPosition(1);
        vc.setPosition(0.5);
        vc.setPosition(0.7);
        vc.open();

        Iterator<ActionItem> iterator = actions.iterator();
//        vc.setPosition(1);
        Assert.assertEquals(new ActionItem(openActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(closeActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(closeActor, "off", valveSwitchDuration, 20), iterator.next());

//        vc.setPosition(0.5);
        Assert.assertEquals(new ActionItem(closeActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(openActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(openActor, "off", (int)(0.5 * valveSwitchDuration), 20), iterator.next());

//        vc.setPosition(0.7);
        Assert.assertEquals(new ActionItem(openActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(closeActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(closeActor, "off", (int)(0.2 * valveSwitchDuration), 20), iterator.next());

//        vc.open();
        Assert.assertEquals(new ActionItem(closeActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(openActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(openActor, "off", (int)((0.7 + LouversControllerImpl.UP_POSITION_RESERVE) * valveSwitchDuration), 20), iterator.next());

        Assert.assertFalse(iterator.hasNext());
    }


    @Test
    public void testValveManipulationOpenClose() {
        ValveController vc = new ValveControllerImpl("vc", "LC", openActor, closeActor, 20_000);

        vc.close();
        Assert.assertEquals(1.0, vc.getPosition(), 0.001);
        Assert.assertFalse(vc.isOpen());
        Assert.assertTrue(vc.isClosed());

        vc.open();
        Assert.assertEquals(0.0, vc.getPosition(), 0.001);
        Assert.assertTrue(vc.isOpen());
        Assert.assertFalse(vc.isClosed());

    }
}
