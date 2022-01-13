package org.chuma.homecontroller.app.servlet;

import org.chuma.homecontroller.controller.device.OutputDevice;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfo;
import org.chuma.homecontroller.base.node.ConnectedDevice;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.Pin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NodeTestRunner extends Thread {
    static Logger log = LoggerFactory.getLogger(NodeTestRunner.class.getName());
    Mode mode = Mode.cycle;
    boolean modeApplied = false;
    List<ConnectedDevice> oldDevices;
    private NodeInfo nodeInfo;
    private Node node;

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

    public void run() {
        oldDevices = node.getDevices();
        node.removeDevices();

        final Pin[] devPins = getPins();

        try {
            final Date oldBootTime = node.getBuildTime();
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
                            for (int i = 0; i < devPins.length; i++) {
                                node.setPinValue(devPins[i], (mode == Mode.fullOn) ? 0 : 1);
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
        for (ConnectedDevice d : oldDevices) {
            node.addDevice(d);
        }
        try {
            node.reset();
        } catch (IOException e) {
            log.error("RESET after test failed.", e);
        }
    }

    private Pin[] getPins() {
        final OutputDevice[] devs = new OutputDevice[]{
                new OutputDevice("TestOutputDevice Conn" + 1, node, 1),
                new OutputDevice("TestOutputDevice Conn2", node, 2),
                new OutputDevice("TestOutputDevice Conn3", node, 3)};

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