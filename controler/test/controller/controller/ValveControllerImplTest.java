package controller.controller;

import java.util.Iterator;

import junit.framework.Assert;
import org.junit.Test;

public class ValveControllerImplTest extends AbstractControllerTest {
    Actor openActor = new Actor("OPEN");
    Actor closeActor = new Actor("CLOSE");

    @Test
    public void testValveClose() throws Exception {
        ValveController vc = new ValveControllerImpl("vc", "LC", openActor, closeActor, 100);

        vc.close();

        Iterator<ActionItem> iterator = actions.iterator();
        Assert.assertEquals(new ActionItem(openActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(closeActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(closeActor, "off", 100, 2), iterator.next());
        Assert.assertTrue(!iterator.hasNext());
    }

    @Test
    public void testValveManipulation() throws Exception {
        ValveController vc = new ValveControllerImpl("vc", "LC", openActor, closeActor, 100);

        vc.close();
        vc.setPosition(100);
        vc.setPosition(50);
        vc.setPosition(70);
        vc.open();

        Iterator<ActionItem> iterator = actions.iterator();
//        vc.setPosition(100);
        Assert.assertEquals(new ActionItem(openActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(closeActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(closeActor, "off", 100, 10), iterator.next());

//        vc.setPosition(100);
//        vc.setPosition(50);
        Assert.assertEquals(new ActionItem(closeActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(openActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(openActor, "off", 50, 10), iterator.next());

//        vc.setPosition(70);
        Assert.assertEquals(new ActionItem(openActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(closeActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(closeActor, "off", 20, 10), iterator.next());

//        vc.open();
        Assert.assertEquals(new ActionItem(closeActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(openActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(openActor, "off", 70 + up100Reserve, 10), iterator.next());

        Assert.assertTrue(!iterator.hasNext());
    }


}