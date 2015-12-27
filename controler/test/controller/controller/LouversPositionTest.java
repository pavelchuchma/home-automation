package controller.controller;

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

        Thread.sleep(5);
        // 99 ms down
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(10, lp.getOffset());

        Thread.sleep(1);
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
        Thread.sleep(17);
        // -9 ms
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(-1, lp.getOffset());

        Thread.sleep(1);
        // -10 ms
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(0, lp.getOffset());

        Thread.sleep(89);
        // -99 ms
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(0, lp.getOffset());

        Thread.sleep(1);
        // -100 ms
        Assert.assertEquals(0, lp.getPosition());
        Assert.assertEquals(0, lp.getOffset());
    }

    @Test
    public void testMovement() throws Exception {
        LouversPosition lp = new LouversPosition(100, 10, 0);

        int i = lp.startDown();
        Assert.assertEquals(100, i);
        Thread.sleep(120);
        Assert.assertEquals(100, lp.getPosition());
        Assert.assertEquals(10, lp.getOffset());
        Assert.assertTrue(lp.isDown());

        i = lp.startUp();
        Assert.assertEquals(100, i);
        Thread.sleep(4);
        Assert.assertEquals(96, lp.getPosition());
        Assert.assertEquals(6, lp.getOffset());
        Assert.assertTrue(lp.isDown());

        Thread.sleep(4);
        Assert.assertEquals(92, lp.getPosition());
        Assert.assertEquals(2, lp.getOffset());
        Assert.assertTrue(lp.isDown());

        Thread.sleep(4);
        Assert.assertEquals(88, lp.getPosition());
        Assert.assertEquals(0, lp.getOffset());
        Assert.assertTrue(!lp.isDown());

        i = lp.startDown();
        Assert.assertEquals(12, i);
        Thread.sleep(4);
        Assert.assertEquals(92, lp.getPosition());
        Assert.assertEquals(4, lp.getOffset());
        Assert.assertTrue(lp.isDown());

        lp.stop();
    }
}