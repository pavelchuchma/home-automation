package controller.controller;

import org.junit.Assert;
import org.junit.Test;

public class LouversPositionTest {
    @Test
    public void testFindDownPosition() throws Exception {
        LouversPosition lp = new LouversPosition(100, 10);
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(-1, lp.getOffset());

        int i = lp.down();
        Assert.assertEquals(100, i);
        Thread.sleep(7);
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(-1, lp.getOffset());

        lp.stop(); // stopped - no change expected
        Thread.sleep(20);
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(-1, lp.getOffset());
        i = lp.down();
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

        lp.up();
        Thread.sleep(4);
        // 91 ms down
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(6, lp.getOffset());

        lp.down();
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
        LouversPosition lp = new LouversPosition(100, 10);
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(-1, lp.getOffset());

        int i = lp.up();
        Assert.assertEquals(100, i);

        Thread.sleep(7);
        // -7 ms
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(-1, lp.getOffset());

        i = lp.down();
        Assert.assertEquals(100, i);
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(-1, lp.getOffset());

        Thread.sleep(15);
        // +8 ms
        Assert.assertEquals(-1, lp.getPosition());
        Assert.assertEquals(-1, lp.getOffset());


        lp.up();
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
}