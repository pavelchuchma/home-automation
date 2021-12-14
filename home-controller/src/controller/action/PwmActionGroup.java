package controller.action;

import controller.actor.Actor;
import controller.actor.PwmActor;
import org.apache.log4j.Logger;


public class PwmActionGroup {
    public static final int STEP_DELAY = 300;
    public static final int FIRST_STEP_DELAY = 600;
    static Logger LOGGER = Logger.getLogger(PwmActionGroup.class.getName());
    int initialPwmValue;
    Action upPressed;
    Action upReleased;
    Action downPressed;
    Action downReleased;

    public PwmActionGroup(PwmActor actor, int initialPwmValue) {
        this.initialPwmValue = initialPwmValue;

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
            getActor().removeActionData();
        }
    }

    private static class UpReleased extends AbstractAction {
        public UpReleased(PwmActor actor) {
            super(actor);
        }

        @Override
        public void perform(int buttonDownDuration) {
            getActor().removeActionData();
        }
    }

    private static class DownPressed extends AbstractAction {
        public DownPressed(Actor actor) {
            super(actor);
        }

        @Override
        public void perform(int buttonDownDuration) {
            PwmActor actor = (PwmActor) getActor();
            int value = actor.getValue();

            if (value != 0) {
                actor.setValue(0, this);
            } else {
                actor.setValue(1, this);
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
            final int initialValue = actor.getValue();

            if (initialValue == 0) {
                actor.setValue(initialPwmValue, this);
            } else {
                actor.increasePwm(10, this);
            }

            try {
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (actor) {
                    actor.wait(FIRST_STEP_DELAY);
                    while (true) {
                        // modified by somebody else or is fully open
                        if (actor.getLastActionData() != this || actor.getValue() == 100) {
                            return;
                        }
                        actor.increasePwm(10, this);
                        actor.wait(STEP_DELAY);
                    }
                }
            } catch (InterruptedException e) {
                LOGGER.error("Unexpected: ", e);
            }
        }
    }
}