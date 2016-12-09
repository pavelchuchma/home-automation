package controller.controller;

import java.util.Iterator;

import junit.framework.Assert;
import org.junit.Test;

public class LouversControllerImplTest extends AbstractControllerTest {
    Actor upActor = new Actor("UP");
    Actor downActor = new Actor("DOWN");

    @Test
    public void testUp() throws Exception {
        LouversControllerImpl lc = new LouversControllerImpl("lc", "LC", upActor, downActor, 100, 10);

        lc.up();

        Assert.assertEquals(3, actions.size());
        Iterator<ActionItem> iterator = actions.iterator();
        Assert.assertEquals(new ActionItem(downActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "off", 100 + up100Reserve, 2), iterator.next());
        Assert.assertTrue(!iterator.hasNext());
    }

    @Test
    public void testDown() throws Exception {
        LouversControllerImpl lc = new LouversControllerImpl("lc", "LC", upActor, downActor, 100, 10);

        lc.blind();

        Iterator<ActionItem> iterator = actions.iterator();
        Assert.assertEquals(new ActionItem(upActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "off", 100, 2), iterator.next());
        Assert.assertTrue(!iterator.hasNext());
    }

    @Test
    public void testDownAndOutshine() throws Exception {
        LouversControllerImpl lc = new LouversControllerImpl("lc", "LC", upActor, downActor, 1000, 100);

        lc.blind();

        actions.clear();

        lc.outshine(0);
        int pos = lc.louversPosition.getPosition();
        int offset = lc.louversPosition.getOffset();
        Assert.assertTrue("pos: " + pos, pos >= 891 && pos <= 905);
        Assert.assertEquals(0, offset);

        lc.outshine(50);
        pos = lc.louversPosition.getPosition();
        offset = lc.louversPosition.getOffset();
        Assert.assertTrue("pos: " + pos, pos >= 941 && pos <= 955);
        Assert.assertTrue("offset: " + offset, offset >= 41 && offset <= 59);

        lc.outshine(70);
        pos = lc.louversPosition.getPosition();
        offset = lc.louversPosition.getOffset();
        Assert.assertTrue("pos: " + pos, pos >= 961 && pos <= 975);
        Assert.assertTrue("offset: " + offset, offset >= 61 && offset <= 79);

        lc.outshine(30);
        pos = lc.louversPosition.getPosition();
        offset = lc.louversPosition.getOffset();
        Assert.assertTrue("pos: " + pos, pos >= 921 && pos <= 935);
        Assert.assertTrue("offset: " + offset, offset >= 21 && offset <= 39);

        Iterator<ActionItem> iterator = actions.iterator();
        Assert.assertEquals(new ActionItem(downActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "off", 100, 5), iterator.next());


//        Assert.assertTrue(!iterator.hasNext());
    }

    @Test
    public void testDownAndOutshineAsync() throws Exception {
        LouversControllerImpl lc = new LouversControllerImpl("lc", "LC", upActor, downActor, 1000, 100);

        lc.up();

        actions.clear();


        Thread thread = new Thread(() -> {
            waitNoException(300);
            lc.outshine(40);
        });
        thread.start();
        long start = System.currentTimeMillis();
        lc.blind();
        thread.join();

        long duration = System.currentTimeMillis() - start;
        Assert.assertTrue("duration: " + duration, duration >= 1040 && duration <= 1100);

        int pos = lc.louversPosition.getPosition();
        Assert.assertTrue("pos: " + pos, pos >= 930 && pos <= 950);
        int offset = lc.louversPosition.getOffset();
        Assert.assertTrue("offset: " + offset, offset >= 31 && offset <= 48);


        Iterator<ActionItem> iterator = actions.iterator();
        // blind()
        Assert.assertEquals(new ActionItem(upActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "on", 0), iterator.next());
        // outshine(20%)
        Assert.assertEquals(new ActionItem(upActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "off", 700, 15), iterator.next()); //remaining (1000-300)
//        Assert.assertEquals(new ActionItem(downActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "off", 60, 10), iterator.next());


        Assert.assertTrue(!iterator.hasNext());
    }

    @Test
    public void testDownAndStop() throws Exception {

        LouversControllerImpl lc = new LouversControllerImpl("lc", "LC", upActor, downActor, 1000, 100);
        lc.up();

        new Thread(() -> {
            waitNoException(300);
            lc.stop();
        }).start();

        long start = System.currentTimeMillis();
        lc.blind();
        long duration = System.currentTimeMillis() - start;
        Assert.assertTrue("duration: " + duration, duration >= 300 && duration < 310);

        Iterator<ActionItem> iterator = actions.iterator();

        Assert.assertEquals(new ActionItem(downActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "off", 1000 + up1000Reserve, 5), iterator.next());


        Assert.assertEquals(new ActionItem(upActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "off", 300, 10), iterator.next());
        Assert.assertTrue(!iterator.hasNext());

        int pos = lc.louversPosition.getPosition();
        Assert.assertTrue("pos: " + pos, pos >= 297 && pos < 310);
        Assert.assertEquals(100, lc.louversPosition.getOffset());
    }

    @Test
    public void testDownAndUp() throws Exception {

        LouversControllerImpl lc = new LouversControllerImpl("lc", "LC", upActor, downActor, 1000, 100);
        lc.up();

        Thread thread = new Thread(() -> {
            waitNoException(300);
            lc.up();
        });
        thread.start();

        long start = System.currentTimeMillis();
        lc.blind();
        long duration = System.currentTimeMillis() - start;
        Assert.assertTrue("duration: " + duration, duration >= 300 && duration < 310);

        thread.join();
        Iterator<ActionItem> iterator = actions.iterator();

        // up()
        Assert.assertEquals(new ActionItem(downActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "off", 1000 + up1000Reserve, 5), iterator.next()); // 100 ms + 5 ms upReserve

        // down()
        Assert.assertEquals(new ActionItem(upActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "on", 0), iterator.next());

        // up() from another thread after 30 ms
        Assert.assertEquals(new ActionItem(downActor, "off", 300, 5), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "off", 300 + up1000Reserve, 5), iterator.next()); //30 ms of position + 5 ms upReserve


        Assert.assertTrue(!iterator.hasNext());

        Assert.assertEquals(0, lc.louversPosition.getPosition());
        Assert.assertEquals(0, lc.louversPosition.getOffset());
    }

    @Test
    public void testDownAndUpBroken() throws Exception {

        LouversControllerImpl lc = new LouversControllerImpl("lc", "LC", upActor, downActor, 1000, 100);
        lc.up();

        Thread thread = new Thread(() -> {
            waitNoException(300);
            downActor.breakIt();
            lc.up();
        });
        thread.start();

        long start = System.currentTimeMillis();
        lc.blind();
        thread.join();
        long duration = System.currentTimeMillis() - start;
        Assert.assertTrue("duration: " + duration, duration >= 300 && duration < 330);

        Iterator<ActionItem> iterator = actions.iterator();

        // up()
        Assert.assertEquals(new ActionItem(downActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "off", 1000 + up1000Reserve, 5), iterator.next());

        // down()
        Assert.assertEquals(new ActionItem(upActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "on", 0), iterator.next());

        // up() from another thread after 30 ms
        Assert.assertEquals(new ActionItem(downActor, "off", 300, 10), iterator.next());
        // broken downActor, no more actions
        Assert.assertTrue(!iterator.hasNext());

        Assert.assertEquals(-1, lc.louversPosition.getPosition());
        Assert.assertEquals(-1, lc.louversPosition.getOffset());
    }

    private void waitNoException(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}