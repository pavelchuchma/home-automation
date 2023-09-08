package org.chuma.homecontroller.controller.controller;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import org.chuma.homecontroller.controller.persistence.PersistentStateMap;
import org.chuma.homecontroller.controller.persistence.StateMap;

@SuppressWarnings("SimplifiableAssertion")
public class PositionTest {

    public static final String STATE_FILE_NAME = "build/tmp/testValue.tmp";

    @Test
    public void testFindUpPosition() throws Exception {
        String positionId = "xyz";

        Position p = new Position(1000, positionId, createEmpty100MsStateMap());

        // start at unknown position
        assertPositionUnknown(p);
        // UP for 100 ms
        int i = p.startUp(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i == 1000);
        Thread.sleep(100);
        p.stop(System.currentTimeMillis());
        assertPositionUnknown(p);

        // up for 800 ms
        i = p.startUp(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i <= 900);
        Thread.sleep(800);
        p.stop(System.currentTimeMillis());
        assertPositionUnknown(p);

        // up for 11 ms to find position
        i = p.startUp(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i <= 100);
        Thread.sleep(110);
        p.stop(System.currentTimeMillis());
        Assert.assertTrue(p.getPositionMs(System.currentTimeMillis()) == 0);
        i = p.startUp(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i == 0);

        // down for 200 ms
        i = p.startDown(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i == 1000);
        Thread.sleep(200);
        p.stop(System.currentTimeMillis());
        // down for 100 ms
        i = p.startDown(System.currentTimeMillis());
        Assert.assertEquals(800, i, 30);
        Thread.sleep(100);
        i = p.startUp(System.currentTimeMillis());
        Assert.assertEquals(300, i, 30);
        Assert.assertEquals(300, p.getPositionMs(System.currentTimeMillis()), 30);
        Thread.sleep(100);
        Assert.assertEquals(200, p.getPositionMs(System.currentTimeMillis()), 30);
        Thread.sleep(100);
        long currentTime = System.currentTimeMillis();
        Assert.assertEquals(100, p.getPositionMs(currentTime), 40);
        i = p.startUp(System.currentTimeMillis());
        Assert.assertEquals(100, i, 40);
        p.stop(System.currentTimeMillis());

        int lastPosition = p.getPositionMs(System.currentTimeMillis());
        // wait for persistence
        Thread.sleep(1500);

        StateMap stateMap2 = new PersistentStateMap(STATE_FILE_NAME, 100);
        Position p2 = new Position(1000, positionId, stateMap2);
        Assert.assertEquals(lastPosition, p2.getPositionMs(System.currentTimeMillis()));
    }

    public static StateMap createEmpty100MsStateMap() {
        File file = new File(STATE_FILE_NAME);
        if (file.exists()) {
            file.delete();
        }
        return new PersistentStateMap(file.getPath(), 100);
    }

    @Test
    public void testFindDownPosition() throws Exception {
        Position p = new Position(1000, "aa", createEmpty100MsStateMap());

        // start at unknown position
        assertPositionUnknown(p);
        // DOWN for 100 ms
        int i = p.startDown(System.currentTimeMillis());
        Assert.assertEquals(1000, i, 0);
        Thread.sleep(100);
        p.stop(System.currentTimeMillis());
        assertPositionUnknown(p);

        // DOWN for 800 ms
        i = p.startDown(System.currentTimeMillis());
        Assert.assertEquals(900, i, 10);
        assertPositionUnknown(p);
        Thread.sleep(800);

        // UP for 200 ms
        i = p.startUp(System.currentTimeMillis());
        Assert.assertEquals(1000, i, 20);
        assertPositionUnknown(p);
        Thread.sleep(200);

        // DOWN for 350 ms
        i = p.startDown(System.currentTimeMillis());
        Assert.assertEquals(300, i, 30);
        assertPositionUnknown(p);
        Thread.sleep(350);

        i = p.startDown(System.currentTimeMillis());
        Assert.assertEquals(0, i);
        i = p.getPositionMs(System.currentTimeMillis());
        Assert.assertEquals(1000, i);
    }

    private static void assertPositionUnknown(Position p) {
        int positionMs = p.getPositionMs(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(positionMs), positionMs < 0);
    }

    @Test
    public void testDownAndUp() throws Exception {
        Position p = new Position(100, "bb", createEmpty100MsStateMap());

        // start at unknown position
        assertPositionUnknown(p);
        // DOWN for 10 ms
        int i = p.startDown(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i == 100);
        Thread.sleep(10);
        i = p.startUp(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i == 100);
        assertPositionUnknown(p);
    }

    @Test
    public void testUpAndDown() throws Exception {
        Position p = new Position(100, "cc", createEmpty100MsStateMap());

        // start at unknown position
        assertPositionUnknown(p);
        // DOWN for 10 ms
        int i = p.startUp(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i == 100);
        Thread.sleep(10);
        i = p.startDown(System.currentTimeMillis());
        Assert.assertTrue(String.valueOf(i), i == 100);
        assertPositionUnknown(p);
    }
}