package org.chuma.homecontroller.controller.controller;

import java.util.function.IntSupplier;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.base.node.NodePin;
import org.chuma.homecontroller.controller.actor.IOnOffActor;
import org.chuma.homecontroller.controller.actor.OnOffActor;

public class LouversControllerImpl implements LouversController {
    public static final double DOWN_POSITION_RESERVE = 0.08;
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
        louversPosition = new LouversPosition(downPositionMs, maxOffsetMs, (int) (downPositionMs * DOWN_POSITION_RESERVE));
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
        log.debug(String.format("'%s': Up", name));
        setPosition(0, 0);
    }

    void setPosition(double downPercent, double offsetPercent) {
        Validate.inclusiveBetween(0., 100., downPercent);
        Validate.inclusiveBetween(0., 100., offsetPercent);

        int desiredPositionMs = (int) (downPercent / 100.0 * louversPosition.position.maxPositionMs);
        int desiredOffsetMs = (int) (offsetPercent / 100.0 * louversPosition.offset.maxPositionMs);
        log.debug(String.format("setPosition to: %d ms, offset: %d ms", desiredPositionMs, desiredOffsetMs));
        Object aData = new Object();
        try {
            synchronized (this) {
                actionData = aData;
                notifyAll();
                int position = louversPosition.getPosition();
                int offset = louversPosition.getOffset();

                while (position < 0 || offset < 0) {
                    log.debug(String.format(" finding position first. Current position: %d, offset: %d", position, offset));
                    // position unknown, move to closer end
                    if (downPercent > 50) {
                        // move down
                        moveImpl(downActor, upActor, louversPosition::startDown, true, aData);
                    } else {
                        // move up
                        moveImpl(upActor, downActor, louversPosition::startUp, true, aData);
                    }
                    position = louversPosition.getPosition();
                    offset = louversPosition.getOffset();
                }

                // validate position is known
                Validate.isTrue(position >= 0);
                Validate.isTrue(offset >= 0);

                boolean needStopConflictingActor = true;
                int posMsDiff = desiredPositionMs - position;
                if (desiredPositionMs == 0 && position != 0) {
                    posMsDiff -= louversPosition.upReserve;
                }
                log.debug(String.format(" Before move: desiredPos: %d, currentPos: %d, diff: %d", desiredPositionMs, position, posMsDiff));
                if (Math.abs(posMsDiff) > louversPosition.position.maxPositionMs * 0.005
                        && (downPercent != 100 || !louversPosition.isDown())) {
                    // position needs a correction
                    moveImpl(posMsDiff, aData, true);
                    position = louversPosition.getPosition();
                    needStopConflictingActor = false;
                    log.debug(String.format(" After move: desiredPos: %d, currentPos: %d, diff: %d", desiredPositionMs, position, desiredPositionMs - position));
                } else {
                    log.debug("  no position change needed");
                }

                offset = louversPosition.getOffset();
                int offMsDiff = desiredOffsetMs - offset;
                log.debug(String.format(" Before move: desiredOff: %d, currentOff: %d, diff: %d", desiredOffsetMs, offset, offMsDiff));

                if (Math.abs(offMsDiff) > louversPosition.offset.maxPositionMs * 0.025) {
                    // offset needs a correction
                    moveImpl(offMsDiff, aData, needStopConflictingActor);
                    offset = louversPosition.getOffset();
                    log.debug(String.format(" After move: desiredOff: %d, currentOff: %d, diff: %d", desiredOffsetMs, offset, desiredOffsetMs - offset));
                } else {
                    log.debug("  no offset change needed");
                }
                position = louversPosition.getPosition();
                log.debug(String.format(" after setPosition: %d (off: %d)", position, offset));
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
     * @throws ExternalModificationException
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
            log.debug(String.format("%s switching on actor '%s' for %d ms", name, actor.getId(), moveMs));
            actor.switchOn(aData);
            wait(actor, aData, moveMs);
            log.debug(String.format("done, switching actor '%s' off", actor.getId()));
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
        log.debug(String.format("'%s': Blind", name));
        setPosition(100, 100);
    }

    @Override
    public synchronized void outshine(int percent) {
        log.debug(String.format("'%s': Outshine %d%%", name, percent));
        setPosition(100, percent);
    }

    @Override
    public void stop() {
        log.debug(String.format("'%s': Stop", name));
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
                throw new RuntimeException("Failed to stop actor " + actor.toString());
            }
        }
    }

    class ExternalModificationException extends Exception {
    }
}