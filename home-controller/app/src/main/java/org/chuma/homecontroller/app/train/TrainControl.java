package org.chuma.homecontroller.app.train;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.node.ListenerManager;
import org.chuma.homecontroller.base.node.NodePin;
import org.chuma.homecontroller.controller.actor.AbstractPinActor;
import org.chuma.homecontroller.controller.actor.PwmActor;

/**
 * Control direction and speed of train.
 */
public class TrainControl {
    private static final int RETRY_COUNT = 3;
    // How long to wait when changing direction. First goes to stop, than wait this time and then sets new direction.
    private static final int DIR_CHANGE_TIMEOUT = 500;
    private static Logger log = LoggerFactory.getLogger(TrainControl.class.getName());

    private String id;
    private ListenerManager<Listener> listenerManager = new ListenerManager<>();
    private NodePin dirLeftPin;
    private NodePin dirRightPin;
    private NodePin speedPin;
    // Direction: -1 left, 0 stop, 1 right
    private volatile int dir;
    // Speed 0-100% (gets converted to "correct" PWM value when passed to PIC)
    private volatile int speed;

    public TrainControl(String id, NodePin dirLeftPin, NodePin dirRightPin, NodePin speedPin) {
        this.id = id;
        this.dirLeftPin = dirLeftPin;
        this.dirRightPin = dirRightPin;
        this.speedPin = speedPin;
    }

    public String getId() {
        return id;
    }

    public NodePin getDirLeftPin() {
        return dirLeftPin;
    }

    public NodePin getDirRightPin() {
        return dirRightPin;
    }

    public NodePin getSpeedPin() {
        return speedPin;
    }

    public void addListener(Listener listener) {
        listenerManager.add(listener);
    }

    public int getDirection() {
        return dir;
    }

    public int getSpeed() {
        return speed;
    }

    /**
     * Direction is -1 for left, 0 for stop, 1 for right.
     */
    public boolean setDirection(int newDir) {
        if (newDir == dir) {
            // No change - do nothing
            return true;
        }
        synchronized (this) {
            log.debug("%s: dir change to %s", id, newDir == 0 ? "stop" : newDir < 0 ? "left" : "right");
            if (dir != 0) {
                // Stop first
                if (!AbstractPinActor.setPinValueImpl(dir < 0 ? dirLeftPin : dirRightPin, 0, RETRY_COUNT)) {
                    return false;
                }
                dir = 0;
                listenerManager.callListeners(l -> l.directionChanged(dir));
                // Wait some time before setting new direction
                try {
                    Thread.sleep(DIR_CHANGE_TIMEOUT);
                } catch(InterruptedException e) {}
            }
            // Set desired direction
            if (newDir != 0 && !AbstractPinActor.setPinValueImpl(newDir < 0 ? dirLeftPin : dirRightPin, 1, RETRY_COUNT)) {
                return false;
            }
            dir = newDir;
        }
        listenerManager.callListeners(l -> l.directionChanged(dir));
        return true;
    }

    /**
     * Set speed, 0 - 100%
     */
    public boolean setSpeed(int newSpeed) {
        if (newSpeed == speed) {
            // No changed - do nothing
            return true;
        }
        // TODO: Gradual increase here?
        synchronized (this) {
            log.debug("%s: speed change to %d%%", id, newSpeed);
            if (!PwmActor.setPinPwmValueImpl(speedPin, newSpeed * 48 / 100, RETRY_COUNT)) {
                return false;
            }
            speed = newSpeed;
        }
        listenerManager.callListeners(l -> l.speedChanged(speed));
        return true;
    }

    public static interface Listener {
        void directionChanged(int dir);
        void speedChanged(int speed);
    }
}
