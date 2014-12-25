package controller.Action;

import controller.actor.PwmActor;
import org.apache.log4j.Logger;


public class PwmActionGroup {
    static Logger LOGGER = Logger.getLogger(PwmActionGroup.class.getName());

    public static final int STEP_DELAY = 300;

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
        return new AbstractAction(actor) {
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
                    while (true) {
                        //noinspection SynchronizationOnLocalVariableOrMethodParameter
                        synchronized (actor) {
                            actor.wait(STEP_DELAY);
                            // modified by somebody else or is fully open
                            if (actor.getLastActionData() != this || actor.getValue() == 100) {
                                return;
                            }
                            actor.increasePwm(10, this);
                        }
                    }
                } catch (InterruptedException e) {
                    LOGGER.error("Unexpected: ", e);
                }
            }
        };
    }

    private AbstractAction createUpReleased(final PwmActor actor) {
        return new AbstractAction(actor) {
            @Override
            public void perform(int buttonDownDuration) {
                getActor().removeActionData();
            }
        };
    }

    private AbstractAction createDownPressed(final PwmActor actor) {
        return new AbstractAction(actor) {
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
        };
    }

    private AbstractAction createDownReleased(final PwmActor actor) {
        return new AbstractAction(actor) {
            @Override
            public void perform(int buttonDownDuration) {
                getActor().removeActionData();
            }
        };
    }
}