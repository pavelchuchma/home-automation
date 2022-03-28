package org.chuma.homecontroller.controller.nodeinfo;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.packet.Packet;

public class NodeInfo {
    private static final Date resetSupportedSince = new GregorianCalendar(2014, 7, 1).getTime();

    private Node node;
    private Date buildTime;
    private Date bootTime;
    private Date lastPingTime;
    private LinkedList<LogMessage> messageLog = new LinkedList<LogMessage>();

    public NodeInfo(Node node) {
        this.node = node;
        this.buildTime = null;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public Date getBuildTime() {
        return buildTime;
    }

    public void setBuildTime(Date buildTime) {
        this.buildTime = buildTime;
    }

    public Date getBootTime() {
        return bootTime;
    }

    public void setBootTime(Date bootTime) {
        this.bootTime = bootTime;
    }

    public Date getLastPingTime() {
        return lastPingTime;
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
            long threshold = System.currentTimeMillis() - 5_000;
            Iterator<LogMessage> i = messageLog.descendingIterator();

            // TODO: There should be some other log cleaner ensure cleaning when this method in never called
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
}