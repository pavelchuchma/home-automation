package org.chuma.homecontroller.controller.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.controller.actor.Actor;
import org.chuma.homecontroller.controller.actor.ActorListener;
import org.chuma.homecontroller.controller.controller.Activity;
import org.chuma.homecontroller.controller.controller.LouversController;


public class LouversActionGroup {
    public static final int TUNING_MODE_DURATION = 5000;
    public static final int LOUVERS_SHADOW_HOLD_TIME = 500;
    // limit button down time to max 5s to don't panic after switch reboot or reconnect
    public static final int MAX_BUTTON_DOWN_DURATION = 5000;
    static Logger log = LoggerFactory.getLogger(LouversActionGroup.class.getName());

    Action upPressed;
    MuteableButtonReleaseLAction upReleased;
    Action downPressed;
    MuteableButtonReleaseLAction downReleased;
    LouversController louversController;

    transient boolean upButtonIsDown;
    transient boolean downButtonIsDown;

    private final SecondaryMode tuningMode;

    public LouversActionGroup(LouversController louversController, ActorListener secondaryModeIndicator) {
        this.louversController = louversController;
        upPressed = new UpPressed();
        upReleased = new UpReleased();
        downPressed = new DownPressed();
        downReleased = new DownReleased();
        tuningMode = new SecondaryMode(TUNING_MODE_DURATION, secondaryModeIndicator);
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

    private void switchTuningModeState() {
        boolean active = tuningMode.switchState();
        downReleased.muteNextAction();
        upReleased.muteNextAction();
        if (!active) {
            // stop movement because it was started by first button pressed when tuning mode was active
            louversController.stop();
        }
    }

    abstract class LAction implements Action {
        @Override
        public Actor getActor() {
            return null;
        }

        public String toString() {
            return String.format("%s@%s(%s)", getClass().getName(), Integer.toHexString(System.identityHashCode(this)), louversController.getLabel());
        }
    }

    private abstract class MuteableButtonReleaseLAction extends LAction {
        private boolean muteNextAction;

        public void muteNextAction() {
            muteNextAction = true;
        }

        protected abstract void performImpl(int buttonDownDuration);

        @Override
        public void perform(int buttonDownDuration) {
            log.debug("{}.perform(mute:{})", this.getClass().getSimpleName(), muteNextAction);
            if (buttonDownDuration < 0 || buttonDownDuration > MAX_BUTTON_DOWN_DURATION) {
                return;
            }
            if (muteNextAction) {
                muteNextAction = false;
                return;
            }
            // ignore button up after too long time
            performImpl(buttonDownDuration);

        }
    }

    private class UpPressed extends LAction {
        @Override
        public void perform(int buttonUpDuration) {
            upButtonIsDown = true;
            if (downButtonIsDown) {
                switchTuningModeState();
            } else if (tuningMode.isActiveAndTouch()) {
                if (louversController.getActivity() != Activity.movingUp) {
                    louversController.up();
                }
            }
        }
    }

    private class UpReleased extends MuteableButtonReleaseLAction {
        @Override
        public void perform(int timeSinceLastAction) {
            upButtonIsDown = false;
            super.perform(timeSinceLastAction);
        }

        @Override
        protected void performImpl(int buttonDownDuration) {
            if (tuningMode.isActiveAndTouch()) {
                louversController.stop();
            } else {
                if (louversController.getActivity() == Activity.movingUp) {
                    louversController.stop();
                } else {
                    louversController.up();
                }
            }
        }
    }

    private class DownPressed extends LAction {
        @Override
        public void perform(int buttonDownDuration) {
            downButtonIsDown = true;
            if (upButtonIsDown) {
                switchTuningModeState();
            } else if (tuningMode.isActiveAndTouch()) {
                if (louversController.getActivity() != Activity.movingDown) {
                    louversController.blind();
                }
            }
        }
    }

    private class DownReleased extends MuteableButtonReleaseLAction {
        @Override
        public void perform(int timeSinceLastAction) {
            downButtonIsDown = false;
            super.perform(timeSinceLastAction);
        }

        @Override
        protected void performImpl(int buttonDownDuration) {
            if (tuningMode.isActiveAndTouch()) {
                louversController.stop();
            } else {
                if (buttonDownDuration > LOUVERS_SHADOW_HOLD_TIME) {
                    louversController.outshine(0);
                } else {
                    if (louversController.getActivity() == Activity.movingDown) {
                        louversController.stop();
                    } else {
                        louversController.blind();
                    }
                }
            }
        }
    }
}