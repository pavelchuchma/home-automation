package org.chuma.homecontroller.app.servlet.rest.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.Pin;
import org.chuma.homecontroller.controller.device.OutputDevice;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfo;

public class NodeTestRunner extends Thread {
    static Logger log = LoggerFactory.getLogger(NodeTestRunner.class.getName());
    private final NodeInfo nodeInfo;
    private final Node node;
    Mode mode = Mode.cycle;
    boolean modeApplied = false;

    public NodeTestRunner(NodeInfo nodeInfo) {
        this.nodeInfo = nodeInfo;
        this.node = nodeInfo.getNode();
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        modeApplied = false;
    }

    @SuppressWarnings("BusyWait")
    public void run() {
        Validate.isTrue(node.getDevices().isEmpty(), "Cannot run test on node %s because it has bound devices", node.getName());
        final Pin[] devPins = getPins();

        try {
            final Date oldBootTime = nodeInfo.getBootTime();
            node.reset();

            for (int i = 0; i < 10 && nodeInfo.getBootTime() == oldBootTime; i++) {
                sleep(500);
            }

            // if reboot was successful
            if (nodeInfo.getBootTime() != oldBootTime) {
                while (mode != Mode.endTest) {
                    if (mode == Mode.cycle) {
                        for (int i = 0; i < devPins.length; i++) {
                            node.setPinValue(devPins[(i - 3 + devPins.length) % devPins.length], 1);
                            node.setPinValue(devPins[i], 0);
                            sleep(150);

                            if (mode != Mode.cycle) {
                                break;
                            }
                        }
                    } else if (mode == Mode.fullOn || mode == Mode.fullOff) {
                        if (!modeApplied) {
                            for (Pin devPin : devPins) {
                                node.setPinValue(devPin, (mode == Mode.fullOn) ? 0 : 1);
                            }
                            modeApplied = true;
                        } else {
                            // nothing to change, sleep
                            sleep(1000);
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error("TEST NODE Interrupted", e);
        } catch (IOException e) {
            log.error("TEST NODE FAILED", e);
        } finally {
            restoreNode();
        }
    }

    private void restoreNode() {
        // restore previous devices
        node.removeDevices();
        try {
            node.reset();
        } catch (IOException e) {
            log.error("RESET after test failed.", e);
        }
    }

    private Pin[] getPins() {
        final OutputDevice[] devs = new OutputDevice[]{
                new OutputDevice("TestOutputDevice Conn1", node, 1),
                new OutputDevice("TestOutputDevice Conn2", node, 2),
                new OutputDevice("TestOutputDevice Conn3", node, 3)
        };

        final Pin[][] devPins = new Pin[3][];
        for (int i = 0; i < 3; i++) {
            devPins[i] = getOutputDevicePins(devs[i]);
        }
        List<Pin> res = new ArrayList<>(18);
        for (int i = 0; i < 6; i++) {
            for (Pin[] pins : devPins) {
                res.add(pins[i]);
            }
        }

        return res.toArray(new Pin[0]);
    }

    Pin[] getOutputDevicePins(OutputDevice dev) {
        return new Pin[]{
                dev.getOut5().getPin(),
                dev.getOut3().getPin(),
                dev.getOut1().getPin(),
                dev.getOut2().getPin(),
                dev.getOut4().getPin(),
                dev.getOut6().getPin()
        };
    }

    public enum Mode {
        cycle,
        fullOn,
        fullOff,
        endTest
    }
}