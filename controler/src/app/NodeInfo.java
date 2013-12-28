package app;

import node.Node;

import java.util.Date;

public class NodeInfo {
    Node node;
    Date buildTime;
    Date bootTime;
    Date lastPingTime;

    public NodeInfo(Node node, Date buildTime) {
        this.node = node;
        this.buildTime = buildTime;
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
}