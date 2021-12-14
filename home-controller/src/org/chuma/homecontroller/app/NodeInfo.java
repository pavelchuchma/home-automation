package org.chuma.homecontroller.app;

import org.chuma.homecontroller.nodes.node.Node;
import org.chuma.homecontroller.nodes.packet.Packet;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;

public class NodeInfo {
    private static final Date resetSupportedSince = new GregorianCalendar(2014, 7, 1).getTime();

    Node node;
    Date buildTime;
    Date bootTime;
    Date lastPingTime;
    private LinkedList<LogMessage> messageLog = new LinkedList<LogMessage>();

    public NodeInfo(Node node) {
        this.node = node;
        this.buildTime = null;
    }

    public Node getNode() {
        return node;
    }

    public Date getBuildTime() {
        return buildTime;
    }

    public Date getBootTime() {
        return bootTime;
    }

    public void setBootTime(Date bootTime) {
        this.bootTime = bootTime;
    }

    public void setLastPingTime(Date lastPingTime) {
        this.lastPingTime = lastPingTime;
    }

    public void addSentLogMessage(Packet packet) {
        synchronized (messageLog) {
            messageLog.addFirst(new LogMessage(packet, false));
        }
    }

    public void addReceivedSentMessage(Packet packet) {
        synchronized (messageLog) {
            messageLog.addFirst(new LogMessage(packet, true));
        }
    }

    public LogMessage[] getMessageLog() {
        synchronized (messageLog) {
            long threshold = new Date().getTime() - 5 * 1000;
            Iterator<LogMessage> i = messageLog.descendingIterator();

            while (i.hasNext()) {
                LogMessage m = i.next();
                if (m.receivedDate < threshold) {
                    i.remove();
                } else {
                    break;
                }
            }
            return messageLog.toArray(new LogMessage[messageLog.size()]);
        }
    }

    public boolean isResetSupported() {
        return buildTime != null && buildTime.after(resetSupportedSince);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}