package org.chuma.homecontroller.controller.nodeinfo.impl;

import junit.framework.TestCase;
import org.junit.Assert;
import static org.apache.commons.lang3.math.NumberUtils.min;

import org.chuma.homecontroller.base.packet.Packet;
import org.chuma.homecontroller.controller.nodeinfo.LogMessage;

public class MessageBufferTest extends TestCase {
    /**
     * Time elapsing test. Tune this value in accord with performance & stability of computer.
     */
    static final int SLEEP_TIME_MS = 300;

    public void testAddEntry() throws InterruptedException {
        final int BUFFER_SIZE = 7;
        MessageBuffer mb = new MessageBuffer(BUFFER_SIZE);

        Assert.assertEquals(0, mb.getMessageLog(1516).length);
        long startTime = System.currentTimeMillis();

        // add 5 messages and sleep after each
        for (int i = 0; i < 5; i++) {
            mb.addEntry(new LogMessage(new Packet(i, 5, null), true));
            Thread.sleep(SLEEP_TIME_MS);
        }

        // [0(0), 1(1t), 2(2t), 3(3*t), 4(4t), -, -], current time = 5t
        Assert.assertEquals(5, mb.getMessageLog(100 * SLEEP_TIME_MS).length);
        for (int i = 0; i < 6; i++) {
            final LogMessage[] messageLog = mb.getMessageLog((int)((i + 0.5) * SLEEP_TIME_MS));
            Assert.assertEquals(i, messageLog.length);
            for (int j = 0; j < i; j++) {
                Assert.assertEquals(5 - i + j, messageLog[j].packet.nodeId);
            }
        }

        for (int i = 5; i < 10; i++) {
            mb.addEntry(new LogMessage(new Packet(i, 5, null), true));
            Thread.sleep(SLEEP_TIME_MS);
        }

        // [7(7t), 8(8t), 9(9t), 3(3*t), 4(4t), 5(5t), 6(6t)], current time = 10t
        final LogMessage[] messageLog3 = mb.getMessageLog(100 * SLEEP_TIME_MS);
        Assert.assertEquals(BUFFER_SIZE, messageLog3.length);
        for (int i = 0; i < 10; i++) {
            final int expCount = min(i, BUFFER_SIZE);
            final LogMessage[] messageLog = mb.getMessageLog((int)((i + 0.5) * SLEEP_TIME_MS));
            Assert.assertEquals(expCount, messageLog.length);
            for (int j = 0; j < expCount; j++) {
                final long expectedId = 10 - expCount + j;
                Assert.assertEquals(expectedId, messageLog[j].packet.nodeId);
                // verify timestamps almost here
                Assert.assertEquals((double)messageLog[j].timestamp, startTime + expectedId * SLEEP_TIME_MS, SLEEP_TIME_MS / 2f);
            }
        }
    }
}