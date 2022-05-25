package org.chuma.homecontroller.controller.controller;

import java.util.function.IntSupplier;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.node.NodePin;
import org.chuma.homecontroller.controller.actor.IOnOffActor;
import org.chuma.homecontroller.controller.actor.OnOffActor;

public class LouversControllerImpl implements LouversController {
    public static final double UP_POSITION_RESERVE = 0.08;
    static Logger log = LoggerFactory.getLogger(LouversControllerImpl.class.getName());
    LouversPosition louversPosition;
    IOnOffActor upActor;
    IOnOffActor downActor;
    String id;
    String name;
    // initialization necessary due to optimized stop()
    Object actionData = new Object();

    public LouversControllerImpl(String id, String name, IOnOffActor upActor, IOnOffActor downActor, int downPositionMs, int maxOffsetMs) {
        init(id, name, upActor, downActor, downPositionMs, maxOffsetMs);
    }

    public LouversControllerImpl(String id, String name, NodePin relayUp, NodePin relayDown, int downPositionMs, int maxOffsetMs) {
        IOnOffActor upActor = new OnOffActor(name + " Up", "LABEL", relayUp, 0, 1);
        IOnOffActor downActor = new OnOffActor(name + " Down", "LABEL", relayDown, 0, 1);

        init(id, name, upActor, downActor, downPositionMs, maxOffsetMs);
    }

    private void init(String id, String name, IOnOffActor upActor, IOnOffActor downActor, int downPositionMs, int maxOffsetMs) {
        this.id = id;
        this.name = name;
        this.upActor = upActor;
        this.downActor = downActor;
        louversPosition = new LouversPosition(downPositionMs, maxOffsetMs, (int) (downPositionMs * UP_POSITION_RESERVE));
    }

    @Override
    public String getLabel() {
        return name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Activity getActivity() {
        return louversPosition.getActivity();
    }

    @Override
    public boolean isUp() {
        return louversPosition.getPosition() == 0;
    }

    @Override
    public boolean isDown() {
        return louversPosition.isDown();
    }

    @Override
    public double getPosition() {
        double pos = louversPosition.getPosition();
        return (pos >= 0) ? pos / louversPosition.position.maxPositionMs : -1;
    }

    @Override
    public double getOffset() {
        double offset = louversPosition.getOffset();
        return (offset >= 0) ? offset / louversPosition.offset.maxPositionMs : -1;
    }

    @Override
    public void up() {
        log.debug("'{}': Up", name);
        setPosition(0, 0);
    }

    /**
     * @param position value from interval <0;1>, where 0 stands for up and 1 for down
     * @param offset   value from interval <0;1>, where 0 stands for open and 1 for closed
     */
    void setPosition(double position, double offset) {
        Validate.inclusiveBetween(0d, 1d, position);
        Validate.inclusiveBetween(0d, 1d, offset);

        int desiredPositionMs = (int) (position * louversPosition.position.maxPositionMs);
        int desiredOffsetMs = (int) (offset * louversPosition.offset.maxPositionMs);
        log.debug("setPosition to: {} ms, offset: {} ms", desiredPositionMs, desiredOffsetMs);
        final Object aData = new Object();
        try {
            synchronized (this) {
                actionData = aData;
                notifyAll();
                int currentPositionMs = louversPosition.getPosition();
                int currentOffsetMs = louversPosition.getOffset();

                while (currentPositionMs < 0 || currentOffsetMs < 0) {
                    log.debug(" finding current position first. Current position: {}, offset: {}", currentPositionMs, currentOffsetMs);
                    // position unknown, move to closer end
                    if (position > 0.5) {
                        // move down
                        moveImpl(downActor, upActor, louversPosition::startDown, true, aData);
                    } else {
                        // move up
                        moveImpl(upActor, downActor, louversPosition::startUp, true, aData);
                    }
                    currentPositionMs = louversPosition.getPosition();
                    currentOffsetMs = louversPosition.getOffset();
                }

                boolean needStopConflictingActor = true;
                int posMsDiff = desiredPositionMs - currentPositionMs;
                if (desiredPositionMs == 0 && currentPositionMs != 0) {
                    posMsDiff -= louversPosition.upReserve;
                }
                log.debug(" Before move: desiredPos: {}, currentPos: {}, diff: {}", desiredPositionMs, currentPositionMs, posMsDiff);
                if (Math.abs(posMsDiff) > louversPosition.position.maxPositionMs * 0.005
                        && (position != 1 || !louversPosition.isDown())) {
                    // position needs a correction
                    moveImpl(posMsDiff, aData, true);
                    currentPositionMs = louversPosition.getPosition();
                    needStopConflictingActor = false;
                    log.debug(" After move: desiredPos: {}, currentPos: {}, diff: {}", desiredPositionMs, currentPositionMs, desiredPositionMs - currentPositionMs);
                } else {
                    log.debug("  no position change needed");
                }

                currentOffsetMs = louversPosition.getOffset();
                int offMsDiff = desiredOffsetMs - currentOffsetMs;
                log.debug(" Before move: desiredOff: {}, currentOff: {}, diff: {}", desiredOffsetMs, currentOffsetMs, offMsDiff);

                if (Math.abs(offMsDiff) > louversPosition.offset.maxPositionMs * 0.025) {
                    // offset needs a correction
                    moveImpl(offMsDiff, aData, needStopConflictingActor);
                    currentOffsetMs = louversPosition.getOffset();
                    log.debug(" After move: desiredOff: {}, currentOff: {}, diff: {}", desiredOffsetMs, currentOffsetMs, desiredOffsetMs - currentOffsetMs);
                } else {
                    log.debug("  no offset change needed");
                }
                currentPositionMs = louversPosition.getPosition();
                log.debug(" after setPosition: {} (off: {})", currentPositionMs, currentOffsetMs);
            }
        } catch (ExternalModificationException e) {
            log.debug("External modification, exiting");
        } catch (Exception e) {
            log.error("Unexpected exception caught, invalidating position info", e);
            louversPosition.invalidate();
            throw e;
        }
    }

    /**
     * @param msDiff negative -> up, positive -> down
     */
    private void moveImpl(int msDiff, Object aData, boolean stopConflictingActor) throws ExternalModificationException {
        if (msDiff > 0) {
            // move down
            louversPosition.startDown();
            moveImpl(downActor, upActor, () -> msDiff, stopConflictingActor, aData);
        } else {
            // move up
            louversPosition.startUp();
            moveImpl(upActor, downActor, () -> -msDiff, stopConflictingActor, aData);
        }
    }

    private void moveImpl(IOnOffActor actor, IOnOffActor conflictingActor, IntSupplier positionAction, boolean stopConflictingActor, Object aData) throws ExternalModificationException {
        actionData = aData;
        if (stopConflictingActor) {
            if (!conflictingActor.switchOff(aData)) {
                throw new RuntimeException("Failed to stop actor '" + conflictingActor.getId() + "'");
            }
        } else {
            // not stopping, but verify last modification was done by me
            if (conflictingActor.getActionData() != aData) {
                throw new ExternalModificationException();
            }
            Validate.isTrue(!conflictingActor.isOn());
        }
        int moveMs = positionAction.getAsInt();
        if (moveMs > 0) {
            log.debug("{} switching on actor '{}' for {} ms", name, actor.getId(), moveMs);
            actor.switchOn(aData);
            log.debug("{} waiting on actor '{}' for {} ms", name, actor.getId(), moveMs);
            wait(actor, aData, moveMs);
            log.debug("done, switching actor '{}' off", actor.getId());
        }
        louversPosition.stop();
        if (!actor.switchOff(aData)) {
            throw new RuntimeException("Failed to stop actor '" + actor.getId() + "'");
        }
    }

    private void wait(IOnOffActor actor, Object aData, int ms) throws ExternalModificationException {
        try {
            this.wait(ms);
            if (actionData != aData || actor.getActionData() != aData) {
                log.debug("Modified by another thread, exiting");
                throw new ExternalModificationException();
            }
        } catch (InterruptedException e) {
            throw new ExternalModificationException();
        }
    }

    @Override
    public synchronized void blind() {
        log.debug("'{}': Blind", name);
        setPosition(1, 1);
    }

    @Override
    public synchronized void outshine(double offset) {
        log.debug("'{}': Outshine {}", name, offset);
        setPosition(1, offset);
    }

    @Override
    public void stop() {
        log.debug("'{}': Stop", name);
        synchronized (this) {
            louversPosition.stop();
            Object stopData = new Object();
            stopActorIfNecessary(upActor, stopData);
            stopActorIfNecessary(downActor, stopData);
            actionData = stopData;
            notifyAll();
        }
    }

    private void stopActorIfNecessary(IOnOffActor actor, Object stopData) {
        if (actor.getActionData() != actionData || actor.isOn()) {
            if (!actor.switchOff(stopData)) {
                throw new RuntimeException("Failed to stop actor " + actor);
            }
        }
    }

    private static class ExternalModificationException extends Exception {
    }
}