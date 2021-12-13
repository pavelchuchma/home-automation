package servlet;

import app.NodeInfo;
import controller.device.ConnectedDevice;
import controller.device.OutputDevice;
import node.Node;
import node.Pin;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NodeTestRunner extends Thread {
    private NodeInfo nodeInfo;
    private Node node;
    static Logger log = Logger.getLogger(NodeTestRunner.class.getName());
    Mode mode = Mode.cycle;
    boolean modeApplied = false;
    List<ConnectedDevice> oldDevices;

    enum Mode {
        cycle,
        fullOn,
        fullOff,
        endTest
    }

    public NodeTestRunner(NodeInfo nodeInfo) {
        this.nodeInfo = nodeInfo;
        this.node = nodeInfo.getNode();
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        modeApplied = false;
    }

    public Mode getMode() {
        return mode;
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
            return;
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
        List<Pin> res = new ArrayList<Pin>(18);
        for (int i = 0; i < 6; i++) {
            for (Pin[] pins : devPins) {
                res.add(pins[i]);
            }
        }

        return res.toArray(new Pin[res.size()]);
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
}