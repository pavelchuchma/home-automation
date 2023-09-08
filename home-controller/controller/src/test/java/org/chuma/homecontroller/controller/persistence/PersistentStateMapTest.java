package org.chuma.homecontroller.controller.persistence;

import java.io.File;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;

import junit.framework.TestCase;
import org.junit.Assert;

public class PersistentStateMapTest extends TestCase {
    public void testPersistence() throws InterruptedException, IOException {
        File file = new File("build/tmp/testValue.tmp");
        if (file.exists()) {
            file.delete();
        }
        PersistentStateMap sp = new PersistentStateMap(file.getPath(), 1_000);
        sp.setValue("aa", 33);
        Thread.sleep(500);
        sp.setValue("bbb", 0);

        {
            PersistentStateMap sp2 = new PersistentStateMap(file.getPath(), 0);
            Assert.assertNull(sp2.getValue("aa"));
        }
        Thread.sleep(700);
        {
            PersistentStateMap sp2 = new PersistentStateMap(file.getPath(), 0);
            Assert.assertEquals(33, sp2.getValue("aa").intValue());
            Assert.assertEquals(0, sp2.getValue("bbb").intValue());
            Assert.assertNull(sp2.getValue("ee"));
        }

        {
            // no value change
            FileTime fileTime1 = Files.readAttributes(file.toPath(), BasicFileAttributes.class).lastModifiedTime();
            sp.setValue("aa", 33);
            Thread.sleep(1_200);
            FileTime fileTime2 = Files.readAttributes(file.toPath(), BasicFileAttributes.class).lastModifiedTime();
            Assert.assertEquals(fileTime1, fileTime2);
        }
        {
            // value changed
            FileTime fileTime1 = Files.readAttributes(file.toPath(), BasicFileAttributes.class).lastModifiedTime();
            sp.setValue("aa", 11111);
            Thread.sleep(1_200);
            FileTime fileTime2 = Files.readAttributes(file.toPath(), BasicFileAttributes.class).lastModifiedTime();
            Assert.assertNotEquals(fileTime1, fileTime2);
        }
        {
            // value removed
            FileTime fileTime1 = Files.readAttributes(file.toPath(), BasicFileAttributes.class).lastModifiedTime();
            sp.removeValue("aa");
            Thread.sleep(1_200);
            FileTime fileTime2 = Files.readAttributes(file.toPath(), BasicFileAttributes.class).lastModifiedTime();
            Assert.assertNotEquals(fileTime1, fileTime2);
            PersistentStateMap sp2 = new PersistentStateMap(file.getPath(), 0);
            Assert.assertNull(sp2.getValue("aa"));
            Assert.assertEquals(0, sp2.getValue("bbb").intValue());
        }
    }
}