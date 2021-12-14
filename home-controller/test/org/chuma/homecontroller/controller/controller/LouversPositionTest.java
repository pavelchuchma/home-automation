package org.chuma.homecontroller.controller.controller;

import org.junit.Assert;
import org.junit.Test;

public class LouversPositionTest {
    @Test
    public void testFindDownPosition() throws Exception {
        LouversPosition lp = new LouversPosition(100, 10, 0);
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(-1, lp.getOffset());

        int i = lp.startDown();
        Assert.assertEquals(100, i);
        Thread.sleep(7);
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(-1, lp.getOffset());

        lp.stop(); // stopped - no change expected
        Thread.sleep(20);
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(-1, lp.getOffset());
        i = lp.startDown();
        Assert.assertEquals(93, i);

        Thread.sleep(8);
        // 15 ms down
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(10, lp.getOffset());

        Thread.sleep(80);
        // 95 ms down
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(10, lp.getOffset());

        lp.stop(); // stopped - no change expected
        Thread.sleep(200);
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(10, lp.getOffset());

        lp.startUp();
        Thread.sleep(4);
        // 91 ms down
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(6, lp.getOffset());

        lp.startDown();
        Thread.sleep(3);
        // 94 ms down
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(9, lp.getOffset());

        Thread.sleep(4);
        // 98 ms down
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(10, lp.getOffset());

        Thread.sleep(2);
        // 100 ms down
        Assert.assertEquals(100, lp.getPosition());
        Assert.assertEquals(10, lp.getOffset());
    }

    @Test
    public void testFindUpperPosition() throws Exception {
        LouversPosition lp = new LouversPosition(100, 10, 0);
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(-1, lp.getOffset());

        int i = lp.startUp();
        Assert.assertEquals(100, i);

        Thread.sleep(7);
        // -7 ms
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(-1, lp.getOffset());

        i = lp.startDown();
        Assert.assertEquals(100, i);
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(-1, lp.getOffset());

        Thread.sleep(15);
        // +8 ms
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(-1, lp.getOffset());


        lp.startUp();
        Thread.sleep(16);
        // -8 ms
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(-1, lp.getOffset());

        Thread.sleep(3);
        // -11 ms
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(0, lp.getOffset());

        Thread.sleep(86);
        // -97 ms
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(0, lp.getOffset());

        Thread.sleep(3);
        // -101 ms
        Assert.assertEquals(0, lp.getPosition());
        Assert.assertEquals(0, lp.getOffset());
    }

    @Test
    public void testMovement() throws Exception {
        LouversPosition lp = new LouversPosition(1000, 100, 0);

        int i = lp.startDown();
        Assert.assertEquals(1000, i);
        Thread.sleep(1200);
        Assert.assertEquals(1000, lp.getPosition());
        Assert.assertEquals(100, lp.getOffset());
        Assert.assertTrue(lp.isDown());

        i = lp.startUp();
        Assert.assertEquals(1000, i);
        Thread.sleep(40);
        assertWithTolerance(960, lp.getPosition(), 3);
        assertWithTolerance(60, lp.getOffset(), 1);
        Assert.assertTrue(lp.isDown());

        Thread.sleep(40);
        assertWithTolerance(920, lp.getPosition(), 3);
        assertWithTolerance(20, lp.getOffset(), 2);
        Assert.assertTrue(lp.isDown());

        Thread.sleep(40);
        assertWithTolerance(880, lp.getPosition(), 3);
        Assert.assertEquals(0, lp.getOffset());
        Assert.assertTrue(!lp.isDown());

        i = lp.startDown();
        assertWithTolerance(120, i, 2);
        Thread.sleep(40);
        assertWithTolerance(920, lp.getPosition(), 3);
        assertWithTolerance(40, lp.getOffset(), 2);
        Assert.assertTrue(lp.isDown());

        lp.stop();
    }

    private void assertWithTolerance(int expected, int position, int tolerance) {
        String message = "expected " + expected + "(" + tolerance + ") but it was " + position;
        Assert.assertTrue(message, expected + tolerance >= position);
        Assert.assertTrue(message, expected - tolerance <= position);
    }
}