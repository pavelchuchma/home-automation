package org.chuma.homecontroller.controller.nodeinfo;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.packet.Packet;

/**
 * Represents {@link Node} in system with additional information like build time,
 * boot time, last ping time and message log (list of log messages).
 */
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

    /**
     * Last ping is last time something was received from the node.
     */
    public Date getLastPingTime() {
        return lastPingTime;
    }

    public void setLastPingTime(Date lastPingTime) {
        this.lastPingTime = lastPingTime;
    }

    /**
     * Log sent message.
     */
    public void addSentLogMessage(Packet packet) {
        synchronized (messageLog) {
            messageLog.addFirst(new LogMessage(packet, false));
        }
    }

    /**
     * Log received message.
     */
    public void addReceivedSentMessage(Packet packet) {
        synchronized (messageLog) {
            messageLog.addFirst(new LogMessage(packet, true));
        }
    }

    /**
     * Get messages logged in last 5 seconds, oldest first. Clears all older messages.
     */
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

    /**
     * Check if node can be reset.
     */
    public boolean isResetSupported() {
        return buildTime != null && buildTime.after(resetSupportedSince);
    }
}