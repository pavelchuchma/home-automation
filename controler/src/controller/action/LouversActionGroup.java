package controller.action;

import controller.actor.Actor;
import controller.controller.LouversController;
import org.apache.log4j.Logger;


public class LouversActionGroup {
    static Logger LOGGER = Logger.getLogger(LouversActionGroup.class.getName());

    Action upPressed;
    Action upReleased;
    Action downPressed;
    Action downReleased;
    LouversController louversController;

    abstract class LAction implements Action {
        @Override
        public Actor getActor() {
            return null;
        }

        public String toString() {
            return String.format("%s@%s(%s)", getClass().getName(), Integer.toHexString(System.identityHashCode(this)), louversController.getName());
        }
    }

    public LouversActionGroup(LouversController louversController) {
        this.louversController = louversController;
        upPressed = new UpPressed();
        upReleased = new UpReleased();
        downPressed = new DownPressed();
        downReleased = new DownReleased();
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

    private class DownReleased extends LAction {
        @Override
        public void perform(int buttonDownDuration) {
            // limit to max 5s to don't panic after switch reboot or reconnect
            if (buttonDownDuration > 2000 && buttonDownDuration < 5000) {
                louversController.outshine(0);
            }
        }
    }

    private class UpReleased extends LAction {
        @Override
        public void perform(int buttonDownDuration) {
        }
    }

    private class UpPressed extends LAction {
        @Override
        public void perform(int buttonUpDuration) {
            switch (louversController.getActivity()) {
                case movingUp:
                    louversController.stop();
                    break;
                case movingDown:
                    louversController.up();
                    break;
                case stopped:
                    louversController.up();
                    break;
            }
        }
    }

    private class DownPressed extends LAction {
        @Override
        public void perform(int buttonDownDuration) {
            switch (louversController.getActivity()) {
                case movingUp:
                    louversController.blind();
                    break;
                case movingDown:
                    louversController.stop();
                    break;
                case stopped:
                    louversController.blind();
                    break;
            }
        }
    }
}