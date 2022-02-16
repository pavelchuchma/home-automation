package org.chuma.homecontroller.controller.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.controller.actor.Actor;
import org.chuma.homecontroller.controller.actor.PwmActor;


public class PwmActionGroup {
    public static final int STEP_DELAY = 300;
    public static final int FIRST_STEP_DELAY = 600;
    static Logger log = LoggerFactory.getLogger(PwmActionGroup.class.getName());
    final double switchOnValue;
    final Action upPressed;
    final Action upReleased;
    final Action downPressed;
    final Action downReleased;

    public PwmActionGroup(PwmActor actor, double switchOnValue) {
        this.switchOnValue = switchOnValue;

        upPressed = createUpPressed(actor);
        upReleased = createUpReleased(actor);
        downPressed = createDownPressed(actor);
        downReleased = createDownReleased(actor);
    }

    public Action getUpButtonDownAction() {
        return upPressed;
    }

    public Action getUpButtonUpAction() {
        return upReleased;
    }

    public Action getDownButtonDownAction() {
        return downPressed;
    }

    public Action getDownButtonUpAction() {
        return downReleased;
    }

    private AbstractAction createUpPressed(final PwmActor actor) {
        return new UpPressed(actor);
    }

    private AbstractAction createUpReleased(final PwmActor actor) {
        return new UpReleased(actor);
    }

    private AbstractAction createDownPressed(final PwmActor actor) {
        return new DownPressed(actor);
    }

    private AbstractAction createDownReleased(final PwmActor actor) {
        return new DownReleased(actor);
    }

    private static class DownReleased extends AbstractAction {
        public DownReleased(PwmActor actor) {
            super(actor);
        }

        @Override
        public void perform(int buttonDownDuration) {
            getActor().setActionData(null);
        }
    }

    private static class UpReleased extends AbstractAction {
        public UpReleased(PwmActor actor) {
            super(actor);
        }

        @Override
        public void perform(int buttonDownDuration) {
            getActor().setActionData(null);
        }
    }

    private static class DownPressed extends AbstractAction {
        public DownPressed(Actor actor) {
            super(actor);
        }

        @Override
        public void perform(int buttonDownDuration) {
            PwmActor actor = (PwmActor) getActor();
            if (actor.isOn()) {
                actor.setValue(0, this);
            } else {
                actor.setValue(0.01, this);
            }
        }
    }

    private class UpPressed extends AbstractAction {
        public UpPressed(Actor actor) {
            super(actor);
        }

        @Override
        public void perform(int buttonUpDuration) {
            final PwmActor actor = (PwmActor) getActor();

            if (actor.isOn()) {
                actor.increasePwm(.10, this);
            } else {
                actor.setValue(switchOnValue, this);
            }

            try {
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (actor) {
                    actor.wait(FIRST_STEP_DELAY);
                    while (true) {
                        // modified by somebody else or is fully open
                        if (actor.getActionData() != this || actor.getValue() == 1d) {
                            return;
                        }
                        actor.increasePwm(.1, this);
                        actor.wait(STEP_DELAY);
                    }
                }
            } catch (InterruptedException e) {
                log.error("Unexpected: ", e);
            }
        }
    }
}