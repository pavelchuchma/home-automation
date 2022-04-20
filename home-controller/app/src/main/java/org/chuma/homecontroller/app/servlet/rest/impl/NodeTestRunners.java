package org.chuma.homecontroller.app.servlet.rest.impl;

import java.util.HashMap;
import java.util.Map;

import org.chuma.homecontroller.controller.nodeinfo.NodeInfo;

public class NodeTestRunners {
    private final Map<NodeInfo, NodeTestRunner> runners = new HashMap<>();

    public synchronized NodeTestRunner.Mode getTestMode(NodeInfo nodeInfo) {
        NodeTestRunner testRunner = runners.get(nodeInfo);
        return (testRunner != null) ? testRunner.getMode() :
                (nodeInfo.getNode().getDevices().isEmpty()) ? NodeTestRunner.Mode.testReady : null;
    }

    public synchronized void startNodeTest(NodeInfo nodeInfo, NodeTestRunner.Mode mode) {
        NodeTestRunner testRunner = runners.get(nodeInfo);
        if (testRunner == null) {
            testRunner = new NodeTestRunner(nodeInfo);
            runners.put(nodeInfo, testRunner);
            testRunner.setMode(mode);
            testRunner.start();
        }
        testRunner.setMode(mode);
    }

    public synchronized void stopNodeTest(NodeInfo nodeInfo) {
        NodeTestRunner testRunner = runners.get(nodeInfo);
        if (testRunner != null) {
            testRunner.setMode(NodeTestRunner.Mode.endTest);
        }
        runners.remove(nodeInfo);
    }
}
