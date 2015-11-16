package controller.controller;

import org.junit.Assert;
import org.junit.Test;

public class PositionTest {
    @Test
    public void testFindUpPosition() throws Exception {
        Position p = new Position(100);

        // start at unknown position
        Assert.assertTrue(p.getPositionMs() < 0);
        // UP for 10 ms
        int i = p.up();
        Assert.assertTrue(String.valueOf(i), i == 100);
        Thread.sleep(10);
        p.stop();
        Assert.assertTrue(p.getPositionMs() < 0);

        // up for 80 ms
        i = p.up();
        Assert.assertTrue(String.valueOf(i), i <= 90);
        Thread.sleep(80);
        p.stop();
        Assert.assertTrue(p.getPositionMs() < 0);

        // up for 11 ms to find position
        i = p.up();
        Assert.assertTrue(String.valueOf(i), i <= 10);
        Thread.sleep(11);
        p.stop();
        Assert.assertTrue(p.getPositionMs() == 0);
        i = p.up();
        Assert.assertTrue(String.valueOf(i), i == 0);

        // down for 20 ms
        i = p.down();
        Assert.assertTrue(String.valueOf(i), i == 100);
        Thread.sleep(20);
        p.stop();
        // down for 10 ms
        i = p.down();
        Assert.assertTrue(String.valueOf(i), i == 80);
        Thread.sleep(10);
        i = p.up();
        Assert.assertTrue(String.valueOf(i), i == 30);
        Assert.assertTrue(p.getPositionMs() == 30);
    }

    @Test
    public void testFindDownPosition() throws Exception {
        Position p = new Position(100);

        // start at unknown position
        Assert.assertTrue(p.getPositionMs() < 0);
        // DOWN for 10 ms
        int i = p.down();
        Assert.assertTrue(String.valueOf(i), i == 100);
        Thread.sleep(10);
        p.stop();
        Assert.assertTrue(p.getPositionMs() < 0);

        // DOWN for 80 ms
        i = p.down();
        Assert.assertTrue(String.valueOf(i), i == 90);
        Assert.assertTrue(p.getPositionMs() < 0);
        Thread.sleep(80);

        // UP for 20 ms
        i = p.up();
        Assert.assertTrue(String.valueOf(i), i == 100);
        Assert.assertTrue(p.getPositionMs() < 0);
        Thread.sleep(20);

        // DOWN for 35 ms
        i = p.down();
        Assert.assertTrue(String.valueOf(i), i == 30);
        Assert.assertTrue(p.getPositionMs() < 0);
        Thread.sleep(35);

        i = p.down();
        Assert.assertTrue(String.valueOf(i), i == 0);
        i = p.getPositionMs();
        Assert.assertTrue(String.valueOf(i), i == 100);
    }

    @Test
    public void testDownAndUp() throws Exception {
        Position p = new Position(100);

        // start at unknown position
        Assert.assertTrue(p.getPositionMs() < 0);
        // DOWN for 10 ms
        int i = p.down();
        Assert.assertTrue(String.valueOf(i), i == 100);
        Thread.sleep(10);
        i = p.up();
        Assert.assertTrue(String.valueOf(i), i == 100);
        Assert.assertTrue(p.getPositionMs() < 0);
    }

    @Test
    public void testUpAndDown() throws Exception {
        Position p = new Position(100);

        // start at unknown position
        Assert.assertTrue(p.getPositionMs() < 0);
        // DOWN for 10 ms
        int i = p.up();
        Assert.assertTrue(String.valueOf(i), i == 100);
        Thread.sleep(10);
        i = p.down();
        Assert.assertTrue(String.valueOf(i), i == 100);
        Assert.assertTrue(p.getPositionMs() < 0);
    }
}