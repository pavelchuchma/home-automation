package org.chuma.homecontroller.controller.controller;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("SimplifiableAssertion")
public class PositionTest {
    @Test
    public void testFindUpPosition() throws Exception {
        Position p = new Position(100);

        // start at unknown position
        Assert.assertTrue(p.getPositionMs(System.currentTimeMillis()) < 0);
        // UP for 10 ms
        int i = p.startUp(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i == 100);
        Thread.sleep(10);
        p.stop(System.currentTimeMillis());
        Assert.assertTrue(p.getPositionMs(System.currentTimeMillis()) < 0);

        // up for 80 ms
        i = p.startUp(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i <= 90);
        Thread.sleep(80);
        p.stop(System.currentTimeMillis());
        Assert.assertTrue(p.getPositionMs(System.currentTimeMillis()) < 0);

        // up for 11 ms to find position
        i = p.startUp(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i <= 10);
        Thread.sleep(11);
        p.stop(System.currentTimeMillis());
        Assert.assertTrue(p.getPositionMs(System.currentTimeMillis()) == 0);
        i = p.startUp(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i == 0);

        // down for 20 ms
        i = p.startDown(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i == 100);
        Thread.sleep(20);
        p.stop(System.currentTimeMillis());
        // down for 10 ms
        i = p.startDown(System.currentTimeMillis());
        Assert.assertEquals(80, i);
        Thread.sleep(10);
        i = p.startUp(System.currentTimeMillis());
        Assert.assertEquals(30, i);
        Assert.assertEquals(30, p.getPositionMs(System.currentTimeMillis()));
        Thread.sleep(10);
        Assert.assertEquals(20, p.getPositionMs(System.currentTimeMillis()));
        Thread.sleep(10);
        long currentTime = System.currentTimeMillis();
        Assert.assertEquals(10, p.getPositionMs(currentTime));
        i = p.startUp(System.currentTimeMillis());
        Assert.assertEquals(10, i);
    }

    @Test
    public void testFindDownPosition() throws Exception {
        Position p = new Position(100);

        // start at unknown position
        Assert.assertTrue(p.getPositionMs(System.currentTimeMillis()) < 0);
        // DOWN for 10 ms
        int i = p.startDown(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i == 100);
        Thread.sleep(10);
        p.stop(System.currentTimeMillis());
        Assert.assertTrue(p.getPositionMs(System.currentTimeMillis()) < 0);

        // DOWN for 80 ms
        i = p.startDown(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i == 90);
        Assert.assertTrue(p.getPositionMs(System.currentTimeMillis()) < 0);
        Thread.sleep(80);

        // UP for 20 ms
        i = p.startUp(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i == 100);
        Assert.assertTrue(p.getPositionMs(System.currentTimeMillis()) < 0);
        Thread.sleep(20);

        // DOWN for 35 ms
        i = p.startDown(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i == 30);
        Assert.assertTrue(p.getPositionMs(System.currentTimeMillis()) < 0);
        Thread.sleep(35);

        i = p.startDown(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i == 0);
        i = p.getPositionMs(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i == 100);
    }

    @Test
    public void testDownAndUp() throws Exception {
        Position p = new Position(100);

        // start at unknown position
        Assert.assertTrue(p.getPositionMs(System.currentTimeMillis()) < 0);
        // DOWN for 10 ms
        int i = p.startDown(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i == 100);
        Thread.sleep(10);
        i = p.startUp(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i == 100);
        Assert.assertTrue(p.getPositionMs(System.currentTimeMillis()) < 0);
    }

    @Test
    public void testUpAndDown() throws Exception {
        Position p = new Position(100);

        // start at unknown position
        Assert.assertTrue(p.getPositionMs(System.currentTimeMillis()) < 0);
        // DOWN for 10 ms
        int i = p.startUp(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i == 100);
        Thread.sleep(10);
        i = p.startDown(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i == 100);
        Assert.assertTrue(p.getPositionMs(System.currentTimeMillis()) < 0);
    }
}