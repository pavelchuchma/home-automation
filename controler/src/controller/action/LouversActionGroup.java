package controller.action;

import controller.actor.Actor;
import controller.controller.Activity;
import controller.controller.LouversController;
import node.NodePin;
import org.apache.log4j.Logger;


public class LouversActionGroup {
    public static final int TUNING_MODE_DURATION = 5000;
    public static final int LOUVERS_SHADOW_HOLD_TIME = 500;
    // limit button down time to max 5s to don't panic after switch reboot or reconnect
    public static final int MAX_BUTTON_DOWN_DURATION = 5000;
    static Logger LOGGER = Logger.getLogger(LouversActionGroup.class.getName());

    Action upPressed;
    MuteableButtonReleaseLAction upReleased;
    Action downPressed;
    MuteableButtonReleaseLAction downReleased;
    LouversController louversController;

    transient boolean upButtonIsDown;
    transient boolean downButtonIsDown;

    private SecondaryMode tuningMode;

    public LouversActionGroup(LouversController louversController, NodePin indicatorPin) {
        this.louversController = louversController;
        upPressed = new UpPressed();
        upReleased = new UpReleased();
        downPressed = new DownPressed();
        downReleased = new DownReleased();
        tuningMode = new SecondaryMode(TUNING_MODE_DURATION, indicatorPin);
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

    private boolean setTuningMode() {
        if (tuningMode.set(upButtonIsDown && downButtonIsDown)) {
            downReleased.muteNextAction();
            upReleased.muteNextAction();
            return true;
        }
        return false;
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
            LOGGER.debug(String.format("%s.perform(mute:%s)", this.getClass().getSimpleName(), muteNextAction));
            if (muteNextAction) {
                muteNextAction = false;
            } else if (buttonDownDuration < MAX_BUTTON_DOWN_DURATION) {
                // ignore button up after too long time
                performImpl(buttonDownDuration);
            }
        }
    }

    private class UpPressed extends LAction {
        @Override
        public void perform(int buttonUpDuration) {
            upButtonIsDown = true;
            if (setTuningMode()) {
                // mode changed (up & down is pressed, don't do any action
                if (!tuningMode.isActive()) {
                    // stop movement because it was started by first button pressed
                    louversController.stop();
                }
            }

            if (tuningMode.isActive()) {
                if (louversController.getActivity() != Activity.movingUp) {
                    louversController.up();
                }
            }
        }
    }

    private class UpReleased extends MuteableButtonReleaseLAction {
        @Override
        public void perform(int previousDurationMs) {
            upButtonIsDown = false;
            super.perform(previousDurationMs);
        }

        @Override
        protected void performImpl(int buttonDownDuration) {
            if (tuningMode.isActive()) {
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
            if (setTuningMode()) {
                // mode changed (up & down is pressed), don't do any action
                if (!tuningMode.isActive()) {
                    // stop movement because it was started by first button pressed
                    louversController.stop();
                }
                return;
            }

            if (tuningMode.isActive()) {
                if (louversController.getActivity() != Activity.movingDown) {
                    louversController.blind();
                }
            }
        }
    }

    private class DownReleased extends MuteableButtonReleaseLAction {
        @Override
        public void perform(int previousDurationMs) {
            downButtonIsDown = false;
            super.perform(previousDurationMs);
        }

        @Override
        protected void performImpl(int buttonDownDuration) {
            if (tuningMode.isActive()) {
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