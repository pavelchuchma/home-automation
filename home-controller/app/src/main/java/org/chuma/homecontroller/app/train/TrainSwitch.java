package org.chuma.homecontroller.app.train;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.node.AsyncListenerManager;
import org.chuma.homecontroller.base.node.ListenerManager;
import org.chuma.homecontroller.base.node.NodePin;
import org.chuma.homecontroller.controller.actor.AbstractPinActor;
import org.chuma.homecontroller.controller.nodeinfo.SwitchListener;

/**
 * Train/rail switch controller. It uses two relay for switching direction
 * (two pins - straight and turn) and two pins for indication of current
 * state. The switch is in given state when pin is 0.
 */
public class TrainSwitch {
    private static final int RETRY_COUNT = 3;
    // How long to wait until switch is correctly switched (max relay activation time)
    private static final int SWITCH_WAIT_TIME = 500;
    // How long to wait for correct state change before sending notification
    private static final int STATE_CHECK_TIME = 100;
    private static Logger log = LoggerFactory.getLogger(TrainSwitch.class.getName());

    private String id;
    private ListenerManager<Consumer<TrainSwitch>> listenerManager = new AsyncListenerManager<>();
    private NodePin switchStraight;
    private NodePin switchTurn;
    private NodePin indicatorStraight;
    private NodePin indicatorTurn;
    private boolean isStraight;
    private boolean isTurn;
    // Guards access to state fields above, also used for wait/notify on state change
    private Object stateLock = new Object();
    // There is also instance lock used to guard reading incomplete state while switching.
    // The instance lock must be always entered before stateLock to avoid deadlock.

    public TrainSwitch(String id, SwitchListener listener, NodePin switchStraight, NodePin switchTurn, NodePin indicatorStraight, NodePin indicatorTurn) {
        this.id = id;
        this.switchStraight = switchStraight;
        this.switchTurn = switchTurn;
        this.indicatorStraight = indicatorStraight;
        this.indicatorTurn = indicatorTurn;
        listener.addActionBinding(new SimpleActionBinding(indicatorStraight, action(() -> isStraight = true), action(() -> isStraight = false)));
        listener.addActionBinding(new SimpleActionBinding(indicatorTurn, action(() -> isTurn = true), action(() -> isTurn = false)));
    }

    public String getId() {
        return id;
    }

    public NodePin getSwitchStraightPin() {
        return switchStraight;
    }

    public NodePin getSwitchTurnPin() {
        return switchTurn;
    }

    public NodePin getIndicatorStraightPin() {
        return indicatorStraight;
    }

    public NodePin getIndicatorTurnPin() {
        return indicatorTurn;
    }

    public void addListener(Consumer<TrainSwitch> listener) {
        listenerManager.add(listener);
    }

    public boolean switchStraight() {
        switchImpl(switchStraight, true, false);
        return isStraight();
    }

    public boolean switchTurn() {
        switchImpl(switchTurn, false, true);
        return isTurn();
    }

    private synchronized void switchImpl(NodePin pin, boolean expStraight, boolean expectedTurn) {
        log.debug("{}: switching to state straight = {}, turn = {}", id, expStraight, expectedTurn);
        // Power on relay - will switch the train switch
        if (!AbstractPinActor.setPinValueImpl(pin, 1, RETRY_COUNT)) {
            // Command not acknowledged
            log.debug("{}: failed to activate relay pin", id);
            return;
        }
        // Wait until switched - but only for limited time
        synchronized (stateLock) {
            if (isStraight != expStraight || isTurn != expectedTurn) {
                log.debug("{}: waiting for switch state change", id);
                try {
                    // Can wait just once since we will be notified when BOTH pins are changed
                    stateLock.wait(SWITCH_WAIT_TIME);
                } catch (InterruptedException e) {}
            }
        }
        // Turn off relay pin (regardless of state)
        AbstractPinActor.setPinValueImpl(pin, 0, RETRY_COUNT);
        log.debug("{}: switching finished", id);
    }

    /**
     * Is switched to straight?
     *
     * @throws IllegalStateException when in unknown state
     */
    public synchronized boolean isStraight() {
        synchronized (stateLock) {
            checkValid();
            return isStraight;
        }
    }

    /**
     * Is switched to turn?
     *
     * @throws IllegalStateException when in unknown state
     */
    public synchronized boolean isTurn() {
        synchronized (stateLock) {
            checkValid();
            return isTurn;
        }
    }

    private void checkValid() {
        if (!(isStraight ^ isTurn)) {
            // Either none or both set - invalid
            throw new IllegalStateException("Unknown switch '" + id + "' state - straight: " + isStraight + ", turn: " + isTurn);
        }
    }

    /**
     * Called when any indicator pin changes state. Passed runnable updates the correct isXXX flag.
     */
    private void onIndicatorChange(Runnable r) {
        boolean notify = false;
        synchronized (stateLock) {
            r.run();
            if (isStraight ^ isTurn) {
                // Notify only when both changed state
                log.debug("{}: switch state changed: straight = {}, turn = {}", id, isStraight, isTurn);
                stateLock.notifyAll();
                notify = true;
            }
        }
        if (notify) {
            // Notify listeners - outside lock
            listenerManager.callListeners(l -> l.accept(TrainSwitch.this));
        } else {
            // Register check notifier in case the switch remains in undetermined state (both false or both true)
            new Thread(() -> errorNotificationCheck()).start();
        }
    }

    private void errorNotificationCheck() {
        synchronized (stateLock) {
            try {
                // Wait some time - or we may be notified when other pin is changed and switch is in correct state
                stateLock.wait(STATE_CHECK_TIME);
            } catch (InterruptedException e) {
            }
            if (!(isStraight ^ isTurn)) {
                // Still in incorrect state - notify
                stateLock.notifyAll();
            } else {
                // Otherwise do nothing, even don't notify since when switch is in correct state, listeners were already called
                return;
            }
        }
        // Notify listeners - outside lock above
        listenerManager.callListeners(l -> l.accept(TrainSwitch.this));
    }

    private IntConsumer action(Runnable r) {
        return (tm) -> onIndicatorChange(r);
    }
}
