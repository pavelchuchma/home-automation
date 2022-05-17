package org.chuma.homecontroller.controller.nodeinfo.impl;

import java.util.ArrayList;

import org.chuma.homecontroller.controller.nodeinfo.LogMessage;

/**
 * Fixed size cyclic synchronized message buffer.
 */
public class MessageBuffer {
    private final LogMessage[] data;
    private int pos = 0;

    public MessageBuffer(int size) {
        data = new LogMessage[size];
    }

    public synchronized void addEntry(LogMessage msg) {
        data[pos] = msg;
        pos = (pos + 1) % data.length;
    }

    /**
     * Returns array of messages with younger than specified maxAgeMs.
     * Array is ordered from the oldest message.
     */
    public synchronized LogMessage[] getMessageLog(int maxAgeMs) {
        ArrayList<LogMessage> tmp = new ArrayList<>(data.length);
        long minTimestamp = System.currentTimeMillis() - maxAgeMs;
        for (int i = 0; i < data.length; i++) {
            LogMessage m = data[(data.length + pos - 1 - i) % data.length];
            if (m == null || m.timestamp < minTimestamp) {
                break;
            }
            tmp.add(m);
        }
        // return new array in reverted order
        LogMessage[] out = new LogMessage[tmp.size()];
        int i = out.length;
        for (LogMessage m : tmp) {
            out[--i] = m;
        }
        return out;
    }
}
