package org.chuma.homecontroller.controller.nodeinfo;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.packet.Packet;
import org.chuma.homecontroller.controller.nodeinfo.impl.MessageBuffer;

/**
 * Represents {@link Node} in system with additional information like build time,
 * boot time, last ping time and message log (list of log messages).
 */
public class NodeInfo {
    private static final Date resetSupportedSince = new GregorianCalendar(2014, Calendar.AUGUST, 1).getTime();

    private Node node;
    private Date buildTime;
    private Date bootTime;
    private Date lastPingTime;
    private final MessageBuffer messageLog = new MessageBuffer(256);

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
        messageLog.addEntry(new LogMessage(packet, false));
    }

    /**
     * Log received message.
     */
    public void addReceivedSentMessage(Packet packet) {
        messageLog.addEntry(new LogMessage(packet, true));
    }

    /**
     * Gets messages logged according to max age or buffer size. Ordered from the oldest message.
     */
    public LogMessage[] getMessageLog(int maxAgeMs) {
        return messageLog.getMessageLog(maxAgeMs);
    }

    /**
     * Check if node can be reset.
     */
    public boolean isResetSupported() {
        return buildTime != null && buildTime.after(resetSupportedSince);
    }
}