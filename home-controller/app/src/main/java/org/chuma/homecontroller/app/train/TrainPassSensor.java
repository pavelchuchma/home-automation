package org.chuma.homecontroller.app.train;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.app.configurator.Options;
import org.chuma.homecontroller.base.node.AsyncListenerManager;
import org.chuma.homecontroller.base.node.ListenerManager;
import org.chuma.homecontroller.base.node.NodePin;
import org.chuma.homecontroller.controller.nodeinfo.SwitchListener;

// TODO: Wrong detection of initial state, or at least when train stands right upon the sensor, trainArrived() is not signalled because there is too short previousDurationMs (at least when fired artificially on init from SwitchListener)
/**
 * Train pass optical sensor. The sensor gives 1 when there is no train and 0 when train is present
 * (or anything which gives enough light reflection to activate it). Optionally pass train control
 * so the class can determine the position of train relative to sensor. Note however that this is
 * more guessing since for longer trains, especially when moving slow, it is impossible to determine
 * when whole train passed the sensor.
 */
public class TrainPassSensor {
    public static final String PROP_FIRE_PERIOD = "train.sensor.fire_period";
    public static final String PROP_TRAIN_PASSED_PERIOD = "train.sensor.train_passed_period";
    private static final Logger log = LoggerFactory.getLogger(TrainPassSensor.class.getName());

    private String id;
    private ListenerManager<Listener> listenerManager = new AsyncListenerManager<>();
    private Options options;
    private NodePin sensorPin;
    private TrainControl trainControl;
    private int firePeriod;
    private boolean optionsFirePeriod;
    private int trainPassedPeriod;
    private boolean optionsTrainPassedPeriod;
    private volatile boolean sensorActive;
    private volatile TrainPosition trainPosition = TrainPosition.UNKNOWN;
    // How long the sensor is inactive - used to determine if train passed it
    private int inactivePeriod;
    // Time from which we are counting inactive period - this one changes when train is stopped
    private long inactiveStartTime;

    /**
     * Train sensor with default fire period.
     */
    public TrainPassSensor(String id, SwitchListener listener, Options options, NodePin sensorPin) {
        this(id, listener, options, sensorPin, options.getInt(PROP_FIRE_PERIOD));
        optionsFirePeriod = true;
    }

    /**
     * Fire period is minimum time (in milliseconds) the sensor must detect "no train"
     * before it fires {@link Listener#trainArrived()} when train arrives.
     */
    public TrainPassSensor(String id, SwitchListener listener, Options options, NodePin sensorPin, int firePeriod) {
        this.id = id;
        this.options = options;
        this.sensorPin = sensorPin;
        this.firePeriod = firePeriod;
        listener.addActionBinding(new SimpleActionBinding(sensorPin, this::onSensorActive, this::onSensorInactive));
        options.addListener((k, v) -> {
            if (optionsFirePeriod) {
                this.firePeriod = options.getInt(PROP_FIRE_PERIOD);
            }
            if (optionsTrainPassedPeriod) {
                trainPassedPeriod = options.getInt(PROP_TRAIN_PASSED_PERIOD);
            }
        });
    }

    /**
     * Configure train position detector with default trainPassedPeriod. See {@link #withTrainPosition(TrainControl, int)}.
     */
    public TrainPassSensor withTrainPosition(TrainControl trainControl) {
        optionsTrainPassedPeriod = true;
        return withTrainPosition(trainControl, options.getInt(PROP_TRAIN_PASSED_PERIOD));
    }

    /**
     * Configure train position detector. It requires train control driving the trains over the sensor.
     * Note that train position guessing is really just guessing since it is not possible to correctly
     * determine when train fully passed the sensor. There are many "holes" in train (coupler, black underframe)
     * so when train is moving too slow or stops, it may looks like it has fully passed already.
     *
     * @param trainPassedPeriod how long (in ms) the sensor must be inactive to act as train has passed
     *        (this is the time used to minimize "holes" in train described above)
     */
    public TrainPassSensor withTrainPosition(TrainControl trainControl, int trainPassedPeriod) {
        this.trainControl = trainControl;
        this.trainPassedPeriod = trainPassedPeriod;
        trainControl.addListener(new TrainControlListener());
        return this;
    }

    public String getId() {
        return id;
    }

    public NodePin getSensorPin() {
        return sensorPin;
    }

    public void addListener(Listener listener) {
        listenerManager.add(listener);
    }

    /**
     * Check if sensor is currently active, i.e. is "seeing" something.
     */
    public boolean isActive() {
        return sensorActive;
    }

    /**
     * Get current train position. Note that it is a best guess, not exact position.
     */
    public TrainPosition getTrainPosition() {
        return trainPosition;
    }

    private void onSensorActive(int previousDurationMs) {
        sensorActive = true;
        boolean notifyPosition = updateTrainPosition();
        if (previousDurationMs >= firePeriod || previousDurationMs < 0) {
            // Was inactive for a long time - fire (-1 is notification for the first time - also fire)
            log.debug("{}: notify TRAIN ARRIVED", id);
            listenerManager.callListeners(l -> l.trainArrived());
        }
        notifyTrainPosition(notifyPosition);
    }

    private void onSensorInactive(int previousDurationMs) {
        sensorActive = false;
        notifyTrainPosition(updateTrainPosition());
    }

    // Train direction or speed changed - notification from TrainController
    private void onTrainChange() {
        notifyTrainPosition(updateTrainPosition());
    }

    // Scheduled timer
    private void timer(long wakeupTime) {
        long waitTime = wakeupTime - System.currentTimeMillis();
        if (waitTime > 0) {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
            }
        }
        notifyTrainPosition(updateTrainPosition());
    }

    private void notifyTrainPosition(boolean doNotify) {
        if (doNotify) {
            log.debug("{}: notify TRAIN POSITION CHANGED: {}", id, trainPosition);
            listenerManager.callListeners(l -> l.positionChanged(trainPosition));
        }
    }

    /**
     * Update train position. Returns true if position listeners should be notified.
     */
    private boolean updateTrainPosition() {
        if (trainControl == null) {
            // No train control - no position tracking
            return false;
        }
        synchronized (this) {
            // Determine the train position - locked to make sure it is atomic
            int dir = trainControl.getDirection();
            if (trainControl.getSpeed() == 0) {
                // Speed 0 is equivalent to stopped
                dir = 0;
            }
            TrainPosition oldPosition = trainPosition;
            if (sensorActive) {
                // Sensor is active
                log.debug("{}: ACTIVE: determining train position: dir = {}, now {}", id, dir, trainPosition);
                if (dir == 0) {
                    // Stopped - is above
                    trainPosition = TrainPosition.ABOVE;
                    log.debug("{}: stopped, new position {}", id, trainPosition);
                } else {
                    // Moving - set according to direction
                    trainPosition = dir < 0 ? TrainPosition.GOING_LEFT : TrainPosition.GOING_RIGHT;
                    log.debug("{}: moving, new position {}", id, trainPosition);
                }
                // Clear inactivity timers
                inactivePeriod = 0;
                inactiveStartTime = 0;
            } else {
                // Sensor not active
                long now = System.currentTimeMillis();
                if (dir == 0) {
                    // Stopped - stop inactivity timer
                    if (inactiveStartTime != 0) {
                        // Was counting - update accumulated time
                        inactivePeriod += (int)(now - inactiveStartTime);
                        inactiveStartTime = 0;
                        log.debug("{}: inactivity stopped, accumulated to far: {}", id, inactivePeriod);
                    }
                } else {
                    // Moving
                    if (inactiveStartTime == 0) {
                        // Either was stopped or just switched to inactive - start counting period
                        inactiveStartTime = now;
                        if (oldPosition == TrainPosition.ABOVE) {
                            // Now moving so change to GOING
                            trainPosition = dir < 0 ? TrainPosition.GOING_LEFT : TrainPosition.GOING_RIGHT;
                        }
                        log.debug("{}: inactivity timer started, position {}", id, trainPosition);
                    } else if (now - inactiveStartTime + inactivePeriod >= trainPassedPeriod) {
                        // Inactive longer than configured period - set position to moved away
                        if (oldPosition == TrainPosition.GOING_LEFT) {
                            trainPosition = TrainPosition.LEFT;
                        } else if (oldPosition == TrainPosition.GOING_RIGHT) {
                            trainPosition = TrainPosition.RIGHT;
                        }
                        // Otherwise nothing - for example when directly above or already away
                        // Set period to limit - won't schedule timer
                        inactivePeriod = trainPassedPeriod;
                        log.debug("{}: sensor inactive longer than period: {} -> {}", id, oldPosition, trainPosition);
                    }
                    // Otherwise continue counting
                    int diff = trainPassedPeriod - inactivePeriod;
                    if (diff > 0) {
                        // Schedule inactivity timer
                        long wakeupTime = now + diff;
                        new Thread(() -> timer(wakeupTime)).start();
                    }
                }
            }
            return oldPosition != trainPosition;
        }
    }

    /**
     * Listener bound to train control to react on train behavior.
     */
    private class TrainControlListener implements TrainControl.Listener {
        @Override
        public void speedChanged(int speed) {
            onTrainChange();
        }

        @Override
        public void directionChanged(int dir) {
            onTrainChange();
        }
    }

    public static enum TrainPosition {
        /**
         * Train position is unknown.
         */
        UNKNOWN,
        /**
         * Train is (probably) left of sensor. It can be still above the sensor because
         * not all parts of train reflect enough light to active the sensor.
         */
        LEFT,
        /**
         * Train is going to the left (is moving and is still above).
         * Note that {@link TrainPassSensor#isActive()} may be false in this state
         * as there is some timeout when sensor can be off. This is because couplers
         * or various parts of wagons may not reflect enough light to active the sensor.  
         */
        GOING_LEFT,
        /**
         * Train is above sensor and is not moving. When it is moving, the state will be
         * either {@link #GOING_LEFT} or {@link #GOING_RIGHT}. Just with GOING states,
         * the sensor may be inactive in this state.
         */
        ABOVE,
        /**
         * Train is going to the right. See also {@link #GOING_LEFT}.
         */
        GOING_RIGHT,
        /**
         * Train is on the right. See also {@link #LEFT}.
         */
        RIGHT
    }

    public static interface Listener {
        /**
         * Called when train was detected after period of time without any.
         * <p>
         * This is the only somewhat reliable information. All the others are just guessing.
         * <p>
         * Note, that train might have stopped on sensor in a way it is no longer "visible"
         * (like coupler above sensor) and when it starts to move again, it will be signaled
         * by this event again. Just because it was stopped for time longer than fire period.
         */
        void trainArrived();
        /**
         * Position of train has changed. Note that passed position could be obsolete so it is
         * probably better to read one from the sensor instance.
         */
        default void positionChanged(TrainPosition position) {};
    }
}
