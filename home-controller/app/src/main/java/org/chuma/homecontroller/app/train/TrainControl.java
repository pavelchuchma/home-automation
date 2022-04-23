package org.chuma.homecontroller.app.train;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.app.configurator.Options;
import org.chuma.homecontroller.base.node.AsyncListenerManager;
import org.chuma.homecontroller.base.node.ListenerManager;
import org.chuma.homecontroller.base.node.NodePin;
import org.chuma.homecontroller.controller.actor.AbstractPinActor;
import org.chuma.homecontroller.controller.actor.PwmActor;

/**
 * Control direction and speed of train.
 */
public class TrainControl {
    private static final int RETRY_COUNT = 3;
    private static final String PROP_DIR_CHANGE_STOP = "train.control.dir_change_stop";
    private static final String PROP_LOWEST_SPEED = "train.control.lowest_speed";
    private static final String PROP_SPEED_CHANGE_STEP = "train.control.speed_change_step";
    private static final String PROP_SPEED_CHANGE_STEP_DURATION = "train.control.speed_change_step_duration";
    private static Logger log = LoggerFactory.getLogger(TrainControl.class.getName());

    private String id;
    private ListenerManager<Listener> listenerManager = new AsyncListenerManager<>();
    private Options options;
    private NodePin dirLeftPin;
    private NodePin dirRightPin;
    private NodePin speedPin;
    // Direction: -1 left, 0 stop, 1 right
    private volatile int dir;
    // Speed 0-100% (gets converted to "correct" PWM value when passed to PIC)
    private volatile int speed;
    // This gets increased with each speed/direction change to detect when to cancel running gradual speed change
    private int era;

    public TrainControl(String id, Options options, NodePin dirLeftPin, NodePin dirRightPin, NodePin speedPin) {
        this.id = id;
        this.options = options;
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
        synchronized (this) {
            era++;
            if (newDir == dir) {
                // No change - do nothing
                return true;
            }
            log.debug("{}: dir change to {}", id, newDir == 0 ? "stop" : newDir < 0 ? "left" : "right");
            if (dir != 0 && speed > 0) {
                // Stop first
                if (!AbstractPinActor.setPinValueImpl(dir < 0 ? dirLeftPin : dirRightPin, 0, RETRY_COUNT)) {
                    return false;
                }
                dir = 0;
                listenerManager.callListeners(l -> l.directionChanged(dir));
                // Wait some time before setting new direction
                try {
                    Thread.sleep(options.getInt(PROP_DIR_CHANGE_STOP));
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
        return setSpeedImpl(newSpeed, null);
    }

    /**
     * Set speed but only if in given era. null era means always and also increases era.
     */
    private boolean setSpeedImpl(int newSpeed, Integer expEra) {
        synchronized (this) {
            if (expEra == null) {
                era++;
            } else if (era != expEra) {
                return false;
            }
            if (newSpeed > 100) {
                newSpeed = 100;
            } else if (newSpeed < 0) {
                newSpeed = 0;
            }
            if (newSpeed == speed) {
                // No changed - do nothing
                return true;
            }
            log.debug("{}: speed change to {}%", id, newSpeed);
            if (!PwmActor.setPinPwmValueImpl(speedPin, newSpeed * 48 / 100, RETRY_COUNT)) {
                return false;
            }
            speed = newSpeed;
        }
        listenerManager.callListeners(l -> l.speedChanged(speed));
        return true;
    }

    /**
     * Incrementally change speed. It slowly increases/decreases speed according to configured parameters.
     */
    public void changeSpeed(int newSpeed) {
        // Apply limits
        if (newSpeed > 100) {
            newSpeed = 100;
        } else if (newSpeed < 0) {
            newSpeed = 0;
        }
        if (speed == newSpeed) {
            // Already at target speed
            return;
        }
        int lowest = options.getInt(PROP_LOWEST_SPEED);
        if (newSpeed < lowest && speed <= lowest) {
            // Request lower speed than lowest and we are also under lowest now - do immediately
            setSpeed(newSpeed);
            return;
        }
        int sleep = options.getInt(PROP_SPEED_CHANGE_STEP_DURATION);
        int stepProp = options.getInt(PROP_SPEED_CHANGE_STEP);
        int step;
        int expEra;
        synchronized (this) {
            // Store current era
            expEra = era;
            step = newSpeed < speed ? -stepProp : stepProp;
        }
        int targetSpeed = newSpeed;
        new Thread(() -> {
            while (true) {
                if (speed == targetSpeed) {
                    // At target speed - we are done
                    return;
                }
                int v = speed + step;
                if (step > 0) {
                    // Accelerate
                    if (speed < lowest) {
                        // Below lowest - jump to lowest
                        v = lowest;
                    } else if (v > targetSpeed) {
                        v = targetSpeed;
                    }
                } else {
                    // Decelerate
                    if (speed <= lowest) {
                        // At or below lowest - stop
                        v = 0;
                    } else if (v < lowest) {
                        v = lowest;
                    }
                }
                if (!setSpeedImpl(v, expEra)) {
                    return;
                }
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }).start();
    }

    public static interface Listener {
        void directionChanged(int dir);
        void speedChanged(int speed);
    }
}
