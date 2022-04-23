package org.chuma.homecontroller.app.train;

import java.util.function.Consumer;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.app.configurator.Options;
import org.chuma.homecontroller.base.node.AsyncListenerManager;
import org.chuma.homecontroller.base.node.ListenerManager;

public class TrainAutodrive  {
    // TODO: Methods should be synchronized but MUST NOT be locked when calling other controls - so best to return "action" which gets executed after the (synchronized) method call finishes
    private static final String PROP_TOP_SPEED = "train.autodrive.top_speed";
    private static final String PROP_DIR_CHANGE_WAIT = "train.autodrive.dir_change_wait";
    private static final Logger log = LoggerFactory.getLogger(TrainAutodrive.class.getName());

    private String id;
    private ListenerManager<Consumer<Boolean>> listenerManager = new AsyncListenerManager<Consumer<Boolean>>();
    private Options options;
    private TrainControl trainControl;
    private TrainSwitch trainSwitch;
    private boolean autodriving;

    // Which sensor we hit the last time (gives info what to do next)
    private volatile int lastSensor;
    // Sensor to which we should arrive
    private volatile int expectedSensor;
    // Last speed - used to detect if slowing down/speeding up as requested
    private volatile int lastSpeed;
    // Required switch position - to check when switching (true = straight, false = turn)
    private volatile boolean switchStraight;
    // State - what command was issued and we await its completion
    // - 1: stop (slow down & stop)
    // - 2: switch train switch if needed
    // - 3: switch direction (via dir 0)
    // - 4: start (speed up) and going - wait for correct sensor, ignore previous sensor (going from opposite direction)
    private volatile int state;

    public TrainAutodrive(String id, Options options, TrainControl trainControl, TrainSwitch trainSwitch, TrainPassSensor ... passSensors) {
        Validate.isTrue(passSensors.length == 3, "Exactly 3 sensors expected");
        this.id = id;
        this.options = options;
        this.trainControl = trainControl;
        this.trainSwitch = trainSwitch;

        trainControl.addListener(new TrainControl.Listener() {
            @Override
            public void directionChanged(int dir) {
                onDirectionChanged(dir);
            }

            @Override
            public void speedChanged(int speed) {
                onSpeedChanged(speed);
            }
        });
        trainSwitch.addListener(sw -> onSwitchChanged());
        for (int i = 0; i < passSensors.length; i++) {
            int n = i;
            passSensors[i].addListener(() -> onSensor(n));
        }
    }

    /**
     * Add listener report autodrive on/off.
     */
    public void addListener(Consumer<Boolean> listener) {
        listenerManager.add(listener);
    }

    /**
     * Start autodriving if possible.
     */
    public boolean startAutodrive() {
        if (!autodriving) {
            // lastSensor contains last sensor the train has hit
            // We should go from that sensor away
            int dir;
            switchStraight = trainSwitch.isStraight();
            if (lastSensor == 0) {
                if (switchStraight) {
                    // Must be in turn - because we probably go from 0 to 2 through switch
                    log.debug("{}: cannot start autodrive, switch position wrong", id);
                    return false;
                }
                expectedSensor = 2;
                dir = 1;
            } else if (lastSensor == 1) {
                if (!switchStraight) {
                    // Must be in strainght - because we probably go from 1 to 2 through switch
                    log.debug("{}: cannot start autodrive, switch position wrong", id);
                    return false;
                }
                expectedSensor = 2;
                dir = 1;
            } else {
                // Going from 2 to either 0 or 1 according to position of switch
                if (switchStraight) {
                    expectedSensor = 1;
                } else {
                    expectedSensor = 0;
                }
                dir = -1;
            }
            // Now determine state according to direction and speed
            // We also need to save current speed so we can determine if we are slowing down/speeding up
            lastSpeed = trainControl.getSpeed();
            int realDir = trainControl.getDirection();
            if (realDir == 0) {
                // Is stopped - make sure speed is 0
                trainControl.setSpeed(0);
                // Do not touch switch, but set direction
                state = 2;
            } else if (realDir == dir) {
                // Direction set - speed up
                if (lastSpeed > 0) {
                    // Already going - keep going
                    state = 4;
                } else {
                    // Speed up
                    state = 3;
                }
            } else {
                // Direction wrong - do not enable auto
                log.debug("{}: cannot start autodrive, direction wrong", id);
                return false;
            }
            // Enable and notify
            autodriving = true;
            listenerManager.callListeners(l -> l.accept(true));
            log.debug("{}: autodrive initialized to state {}, dir was {}, going to sensor {}", id, state, realDir, expectedSensor);
            // Just to correct method according to state
            if (state == 2) {
                onSwitchChanged();
            } else if (state == 3) {
                onDirectionChanged(realDir);
            }
        }
        return true;
    }

    /**
     * Stop autodriving but do NOT stop the train.
     */
    public void stopAutodrive() {
        if (autodriving) {
            cancelAutodrive();
        }
    }

    public boolean isAutodriving() {
        return autodriving;
    }

    private void onDirectionChanged(int dir) {
        if (autodriving) {
            if (state == 3) {
                // Direction change expected
                if (dir == 0) {
                    // Went to stopped state - do nothing
                    return;
                }
                int expDir = lastSensor < 2 ? 1 : -1;
                if (dir == expDir) {
                    // Speed up
                    schedule(options.getInt(PROP_DIR_CHANGE_WAIT), () -> {
                        if (autodriving) {
                            state = 4;
                            log.debug("{}: direction changed to {}, speeding up", id, dir);
                            trainControl.changeSpeed(options.getInt(PROP_TOP_SPEED));
                        }
                    });
                } else {
                    // Wrong direction - just cancel as the train is stopped
                    log.debug("{}: unexpected change of direction {} (expected {}) - cancelling autodrive", id, dir, expDir);
                    cancelAutodrive();
                }
            } else {
                // Change direction 
                log.debug("{}: unexpected change of direction (state {}, dir {}) - STOP", id, state, dir);
                emergencyStop();
            }
        }
    }

    private void schedule(int timeout, Runnable run) {
        new Thread(() -> {
            try {
                Thread.sleep(timeout);
                run.run();
            } catch (InterruptedException e) {
            }
        }).start();
    }

    private void onSpeedChanged(int speed) {
        if (autodriving) {
            if (state == 1) {
                // Slowing down
                if (speed >= lastSpeed) {
                    // Not slowing down - stop
                    log.debug("{}: not slowing down (now {}, before {}) - STOP", id, speed, lastSpeed);
                    emergencyStop();
                } else {
                    lastSpeed = speed;
                    if (speed == 0) {
                        // Stopped - select next sensor and set train switch
                        state = 2;
                        switch (lastSensor) {
                            case 0:
                                switchStraight = false;
                                expectedSensor = 2;
                                break;
                            case 1:
                                switchStraight = true;
                                expectedSensor = 2;
                                break;
                            case 2:
                                switchStraight = !trainSwitch.isStraight();
                                expectedSensor = switchStraight ? 1 : 0;
                                break;
                        }
                        log.debug("{}: stopped, setting switch to {}", id, switchStraight ? "straight" : "turn");
                        if (trainSwitch.isStraight() == switchStraight) {
                            // Already switched - continue by invoking onSwitchChanged()
                            onSwitchChanged();
                        } else if (switchStraight) {
                            trainSwitch.switchStraight();
                        } else {
                            trainSwitch.switchTurn();
                        }
                    }
                }
            } else if (state == 4 && speed > lastSpeed) {
                // Speeding up
                lastSpeed = speed;
            } else {
                // Someone is messing with speed - cancel autodrive but don't stop
                log.debug("{}: unexpected speed change (state {}, speed {}, last {}) - cancel autodrive", id, state, speed, lastSpeed);
                cancelAutodrive();
            }
        }
    }

    private void onSensor(int sensor) {
        if (autodriving) {
            if (sensor == lastSensor) {
                // Do nothing - we just hit original sensor from the opposite direction
            } else if (state == 4 && sensor == expectedSensor) {
                // Hit expected sensor while going to it
                state = 1;
                lastSensor = sensor;
                log.debug("{}: hit expected sensor {}, stop", id, (char)('A' + sensor));
                trainControl.changeSpeed(0);
            } else {
                // Unexpected sensor or in unexpected state - something is wrong
                log.debug("{}: hit unexpected sensor {} (expected {}) - STOP", id, (char)('A' + sensor), (char)('A' + expectedSensor));
                emergencyStop();
            }
        } else {
            lastSensor = sensor;
        }
    }

    private void onSwitchChanged() {
        if (autodriving) {
            if (state == 2 && trainSwitch.isStraight() == switchStraight) {
                // Switched to expected state - change direction
                state = 3;
                int dir = lastSensor < 2 ? 1 : -1;
                log.debug("{}: switched, changing direction to {}", id, dir);
                trainControl.setDirection(dir);                        
            } else {
                // Switch to wrong position - stop
                log.debug("{}: switched to {} unexpectedly - STOP", id, trainSwitch.isStraight() ? "straight" : "turn");
                emergencyStop();
            }
        }
    }

    /**
     * Cancel autodrive but don't stop train.
     */
    private void cancelAutodrive() {
        autodriving = false;
        listenerManager.callListeners(l -> l.accept(false));
    }

    /**
     * Immediately cancel autodrive and stop train.
     */
    private void emergencyStop() {
        autodriving = false;
        trainControl.setSpeed(0);
        trainControl.setDirection(0);
        // Notify after stopping not to lose time (although notifications are async)
        listenerManager.callListeners(l -> l.accept(false));
    }
}
