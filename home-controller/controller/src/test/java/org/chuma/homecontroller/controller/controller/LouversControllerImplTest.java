package org.chuma.homecontroller.controller.controller;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class LouversControllerImplTest extends AbstractControllerTest {
    final int louversLength = 1_000;
    final int offsetLength = 100;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    Actor upActor = new Actor("UP");
    Actor downActor = new Actor("DOWN");

    private int getUpDurationWithTolerance() {
        return (int)(louversLength * (1 + LouversControllerImpl.UP_POSITION_RESERVE));
    }

    @Test
    public void testUp() {
        LouversControllerImpl lc = new LouversControllerImpl("lc", "LC", upActor, downActor, 1000, 10);

        lc.up();

        assertEquals(3, actions.size());
        Iterator<ActionItem> iterator = actions.iterator();
        assertEquals(new ActionItem(downActor, "off", 0), iterator.next());
        assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        assertEquals(new ActionItem(upActor, "off", getUpDurationWithTolerance(), 15), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testDown() {
        LouversControllerImpl lc = new LouversControllerImpl("lc", "LC", upActor, downActor, louversLength, offsetLength);

        lc.blind();

        Iterator<ActionItem> iterator = actions.iterator();
        assertEquals(new ActionItem(upActor, "off", 0), iterator.next());
        assertEquals(new ActionItem(downActor, "on", 0), iterator.next());
        assertEquals(new ActionItem(downActor, "off", louversLength, 15), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testDownAndOutshine() {
        LouversControllerImpl lc = new LouversControllerImpl("lc", "LC", upActor, downActor, louversLength, offsetLength);

        lc.blind();

        actions.clear();

        lc.outshine(0);
        int pos = lc.louversPosition.getPosition();
        int offset = lc.louversPosition.getOffset();
        assertEquals("pos", louversLength - offsetLength, pos, 20d);
        assertEquals(0, offset);

        double targetOffset = 0.5;
        lc.outshine(targetOffset);
        pos = lc.louversPosition.getPosition();
        offset = lc.louversPosition.getOffset();
        assertEquals("pos", louversLength - ((1 - targetOffset) * offsetLength), pos, 16d);
        assertEquals("offset", targetOffset * offsetLength, offset, 20d);

        targetOffset = 0.7;
        lc.outshine(targetOffset);
        pos = lc.louversPosition.getPosition();
        offset = lc.louversPosition.getOffset();
        assertEquals("pos", louversLength - ((1 - targetOffset) * offsetLength), pos, 16d);
        assertEquals("offset", targetOffset * offsetLength, offset, 20d);

        targetOffset = 0.3;
        lc.outshine(targetOffset);
        pos = lc.louversPosition.getPosition();
        offset = lc.louversPosition.getOffset();
        assertEquals("pos", louversLength - ((1 - targetOffset) * offsetLength), pos, 20d);
        assertEquals("offset", targetOffset * offsetLength, offset, 20d);

        Iterator<ActionItem> iterator = actions.iterator();
        assertEquals(new ActionItem(downActor, "off", 0), iterator.next());
        assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        assertEquals(new ActionItem(upActor, "off", 100, 20), iterator.next());


//        Assert.assertTrue(!iterator.hasNext());
    }

    @Test
    public void testDownAndOutshineAsync() throws Exception {
        final double testOffset = 0.4;
        LouversControllerImpl lc = new LouversControllerImpl("lc", "LC", upActor, downActor, louversLength, offsetLength);
        lc.up();
        actions.clear();

        // do blind, but set outshine after 300ms
        final ScheduledFuture<?> future = scheduler.schedule(() -> lc.outshine(testOffset), 300, TimeUnit.MILLISECONDS);
        long start = System.currentTimeMillis();
        lc.blind();
        Assert.assertNull(future.get());

        long duration = System.currentTimeMillis() - start;
        assertEquals("duration", louversLength + (1 - testOffset) * offsetLength, duration, 30d);

        int pos = lc.louversPosition.getPosition();
        assertEquals("pos", pos, louversLength - (1 - testOffset) * offsetLength, 20d);
        int offset = lc.louversPosition.getOffset();
        assertEquals("offset", offset, testOffset * offsetLength, 20d);


        Iterator<ActionItem> iterator = actions.iterator();
        // blind()
        assertEquals(new ActionItem(upActor, "off", 0), iterator.next());
        assertEquals(new ActionItem(downActor, "on", 0), iterator.next());
        // outshine(20%)
        assertEquals(new ActionItem(upActor, "off", 0), iterator.next());
        assertEquals(new ActionItem(downActor, "on", 0), iterator.next());
        assertEquals(new ActionItem(downActor, "off", 700, 15), iterator.next()); //remaining (1000-300)
//        Assert.assertEquals(new ActionItem(downActor, "off", 0), iterator.next());
        assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        assertEquals(new ActionItem(upActor, "off", 60, 10), iterator.next());


        assertFalse(iterator.hasNext());
    }

    @Test
    public void testDownAndStop() throws Exception {
        LouversControllerImpl lc = new LouversControllerImpl("lc", "LC", upActor, downActor, louversLength, offsetLength);
        lc.up();

        final ScheduledFuture<?> future = scheduler.schedule(lc::stop, 300, TimeUnit.MILLISECONDS);

        long start = System.currentTimeMillis();
        lc.blind();
        Assert.assertNull(future.get());
        long duration = System.currentTimeMillis() - start;
        assertEquals("duration: " + duration, duration, 300, 20);

        Iterator<ActionItem> iterator = actions.iterator();

        assertEquals(new ActionItem(downActor, "off", 0), iterator.next());
        assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        assertEquals(new ActionItem(upActor, "off", getUpDurationWithTolerance(), 20), iterator.next());


        assertEquals(new ActionItem(upActor, "off", 0), iterator.next());
        assertEquals(new ActionItem(downActor, "on", 0), iterator.next());
        assertEquals(new ActionItem(downActor, "off", 300, 10), iterator.next());
        assertFalse(iterator.hasNext());

        int pos = lc.louversPosition.getPosition();
        assertEquals("pos: " + pos, 300, pos, 20d);
        assertEquals(100, lc.louversPosition.getOffset());
    }

    @Test
    public void testDownAndUp() throws Exception {
        LouversControllerImpl lc = new LouversControllerImpl("lc", "LC", upActor, downActor, louversLength, offsetLength);
        lc.up();

        final ScheduledFuture<?> future = scheduler.schedule(lc::up, 300, TimeUnit.MILLISECONDS);

        long start = System.currentTimeMillis();
        lc.blind();
        long duration = System.currentTimeMillis() - start;
        assertEquals("duration", 300, duration, 16d);

        Assert.assertNull(future.get());
        Iterator<ActionItem> iterator = actions.iterator();

        // up()
        assertEquals(new ActionItem(downActor, "off", 0), iterator.next());
        assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        assertEquals(new ActionItem(upActor, "off", getUpDurationWithTolerance(), 30), iterator.next()); // 100 ms + 5 ms upReserve

        // down()
        assertEquals(new ActionItem(upActor, "off", 0), iterator.next());
        assertEquals(new ActionItem(downActor, "on", 0), iterator.next());

        // up() from another thread after 30 ms
        assertEquals(new ActionItem(downActor, "off", 300, 30), iterator.next());
        assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        assertEquals(new ActionItem(upActor, "off", (int)(300 + louversLength * LouversControllerImpl.UP_POSITION_RESERVE), 30), iterator.next()); //30 ms of position + 5 ms upReserve


        assertFalse(iterator.hasNext());

        assertEquals(0, lc.louversPosition.getPosition());
        assertEquals(0, lc.louversPosition.getOffset());
    }

    @Test
    public void testDownAndUpBroken() {
        LouversControllerImpl lc = new LouversControllerImpl("lc", "LC", upActor, downActor, louversLength, offsetLength);
        lc.up();

        final ScheduledFuture<?> future = scheduler.schedule(
                () -> {
                    downActor.breakIt();
                    lc.up();
                }, 300, TimeUnit.MILLISECONDS);


        long start = System.currentTimeMillis();
        lc.blind();
        // expects failure
        Assert.assertThrows(ExecutionException.class, future::get);
        long duration = System.currentTimeMillis() - start;
        assertEquals("duration", 300, duration, 20d);

        Iterator<ActionItem> iterator = actions.iterator();

        // up()
        assertEquals(new ActionItem(downActor, "off", 0), iterator.next());
        assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        assertEquals(new ActionItem(upActor, "off", getUpDurationWithTolerance(), 15), iterator.next());

        // down()
        assertEquals(new ActionItem(upActor, "off", 0), iterator.next());
        assertEquals(new ActionItem(downActor, "on", 0), iterator.next());

        // up() from another thread after 30 ms
        assertEquals(new ActionItem(downActor, "off", 300, 20), iterator.next());
        // broken downActor, no more actions
        assertFalse(iterator.hasNext());

        assertEquals(-1, lc.louversPosition.getPosition());
        assertEquals(-1, lc.louversPosition.getOffset());
    }
}