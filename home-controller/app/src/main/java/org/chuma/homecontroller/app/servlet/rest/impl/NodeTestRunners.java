package org.chuma.homecontroller.app.servlet.rest.impl;

import java.util.HashMap;
import java.util.Map;

import org.chuma.homecontroller.controller.nodeinfo.NodeInfo;

public class NodeTestRunners {
    private final Map<NodeInfo, NodeTestRunner> runners = new HashMap<>();

    /**
     * Returns current test mode is active. Otherwise, it returns {@link NodeTestRunner.Mode#testReady} if test
     * is applicable (node has no devices) or null.
     */
    public synchronized NodeTestRunner.Mode getTestMode(NodeInfo nodeInfo) {
        NodeTestRunner testRunner = runners.get(nodeInfo);
        return (testRunner != null) ? testRunner.getMode() :
                (nodeInfo.getNode().getDevices().isEmpty() && !nodeInfo.getNode().isBridgeNode())
                        ? NodeTestRunner.Mode.testReady : null;
    }

    public synchronized void setTestMode(NodeInfo nodeInfo, NodeTestRunner.Mode mode) {
        NodeTestRunner testRunner = runners.get(nodeInfo);
        if (testRunner == null) {
            if (mode == NodeTestRunner.Mode.endTest) {
                // no action needed, test is not running
                return;
            }
            testRunner = new NodeTestRunner(nodeInfo);
            runners.put(nodeInfo, testRunner);
            testRunner.setMode(mode);
            testRunner.start();
        }
        testRunner.setMode(mode);
        if (mode == NodeTestRunner.Mode.endTest) {
            runners.remove(nodeInfo);
        }
    }
}
