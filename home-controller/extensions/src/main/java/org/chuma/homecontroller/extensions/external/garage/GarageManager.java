package org.chuma.homecontroller.extensions.external.garage;

import org.slf4j.Logger;

import org.chuma.homecontroller.controller.action.Action;
import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class GarageManager {
    static Logger log = org.slf4j.LoggerFactory.getLogger(GarageManager.class.getName());

    State state = State.UNKNOWN;
    PreviousMove previousMove = PreviousMove.UNKNOWN;
    Action pulseAction;
    IOnOffActor pulseActor;

    public GarageManager(Action pulseAction) {
        this.pulseAction = pulseAction;
    }

    public GarageManager(IOnOffActor pulseActor) {
        this.pulseActor = pulseActor;
    }

    public State getState() {
        return state;
    }

    public synchronized void doPulse(int count) {
        log.debug("doPulse({})", count);
        for (int i = 0; i < count; i++) {
            try {
                log.debug("actor ON");
                pulseActor.switchOn(null);
                wait(250);
                log.debug("actor OFF");
                pulseActor.switchOff(null);
                wait(300);
            } catch (InterruptedException e) {
                // no action
            }
        }
    }

    public enum State {
        UNKNOWN, OPEN, CLOSED, OPENING, CLOSING, STOPPED;
    }

    public enum PreviousMove {
        UNKNOWN, OPENING, CLOSING;
    }


    public synchronized void open() {
        switch (state) {
            case OPEN -> log.debug("Garage is already open");
            case CLOSED -> {
                log.debug("Garage is closed, opening");
                doPulse(1);
                state = State.OPENING;
            }
            case OPENING -> {
                log.debug("Garage is already opening, stop it");
                doPulse(1);
                state = State.STOPPED;
                previousMove = PreviousMove.OPENING;
            }
            case CLOSING -> {
                log.debug("Garage is already closing, stop and open it");
                doPulse(2);
                state = State.OPENING;
            }
            case STOPPED -> {
                switch (previousMove) {
                    case OPENING -> {
                        log.debug("Garage is stopped, previous was opening, open it");
                        doPulse(3);
                        state = State.OPENING;
                    }
                    case CLOSING -> {
                        log.debug("Garage is stopped, previous was closing, open it");
                        doPulse(1);
                        state = State.OPENING;
                    }
                    case UNKNOWN -> {
                        log.debug("Garage is stopped, previous was unknown, try to open it");
                        doPulse(1);
                        state = State.UNKNOWN;
                    }
                }
                state = State.OPENING;
            }
            case UNKNOWN -> {
                log.debug("Garage is in unknown state, try open it");
                doPulse(1);
                state = State.UNKNOWN;
            }
        }
        log.trace("Garage state after open(): {}", state);
    }

    public synchronized void close() {
        switch (state) {
            case OPEN -> {
                log.debug("Garage is already open, close it");
                doPulse(1);
                state = State.CLOSING;
            }
            case CLOSED -> log.debug("Garage is already closed");
            case OPENING -> {
                log.debug("Garage is opening, stop it and close it");
                doPulse(2);
                state = State.CLOSING;
            }
            case CLOSING -> {
                log.debug("Garage is already closing, stop it");
                doPulse(1);
                state = State.STOPPED;
                previousMove = PreviousMove.CLOSING;
            }
            case STOPPED -> {
                switch (previousMove) {
                    case OPENING -> {
                        log.debug("Garage is stopped, previous was opening, close it");
                        doPulse(1);
                        state = State.CLOSING;
                    }
                    case CLOSING -> {
                        log.debug("Garage is stopped, previous was closing, close it");
                        doPulse(3);
                        state = State.CLOSING;
                    }
                    case UNKNOWN -> {
                        log.debug("Garage is stopped, previous was unknown, try to close it");
                        doPulse(1);
                        state = State.UNKNOWN;
                    }
                }
            }
            case UNKNOWN -> {
                log.debug("Garage is in unknown state, try close it");
                doPulse(1);
                state = State.UNKNOWN;
            }
        }
        log.trace("Garage state after close(): {}", state);
    }

    public synchronized void onOpenContactPressed() {
        state = State.OPEN;
        log.debug("onOpenContactPressed() state={}", state);
    }

    public synchronized void onOpenContactReleased() {
        state = State.CLOSING;
        log.debug("onOpenContactReleased() state={}", state);
    }

    public synchronized void onClosedContactPressed() {
        state = State.CLOSED;
        log.debug("onClosedContactPressed() state={}", state);
    }

    public synchronized void onClosedContactReleased() {
        state = State.OPENING;
        log.debug("onClosedContactReleased() state={}", state);
    }
}
