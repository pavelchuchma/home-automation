package controller.controller;

import java.util.ArrayList;
import java.util.List;

import controller.actor.IOnOffActor;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class AbstractControllerTest {
    List<ActionItem> actions = new ArrayList<>();
    int up100Reserve = (int) (100 * LouversControllerImpl.DOWN_POSITION_RESERVE);
    int up1000Reserve = (int) (1000 * LouversControllerImpl.DOWN_POSITION_RESERVE);

    static class ActionItem {
        IOnOffActor actor;
        String actionName;
        int value;
        int tolerance;

        ActionItem(IOnOffActor actor, String actionName, int value) {
            this(actor, actionName, value, 0);
        }

        ActionItem(IOnOffActor actor, String actionName, int value, int tolerance) {
            this.actor = actor;
            this.actionName = actionName;
            this.value = value;
            this.tolerance = tolerance;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ActionItem that = (ActionItem) o;

            if (Math.abs(value - that.value) > tolerance) return false;
            if (actionName != null ? !actionName.equals(that.actionName) : that.actionName != null) return false;
            if (actor != that.actor) return false;

            return true;
        }

        @Override
        public String toString() {
            return "ActionItem{" +
                    "actor=" + actor +
                    ", actionName='" + actionName + '\'' +
                    ", value=" + value +
                    "(" + tolerance + ")" +
                    '}';
        }
    }

    public class Actor implements IOnOffActor {
        boolean active = false;
        Object actionData;
        long switchOnTime;
        boolean broken = false;
        private String id;

        public Actor(String id) {
            this.id = id;
        }

        public void breakIt() {
            broken = true;
        }

        @Override
        public boolean switchOn(int percent, Object actionData) {
            active = true;
            this.actionData = actionData;
            switchOnTime = System.currentTimeMillis();
            actions.add(new ActionItem(this, "on", 0));
            return !broken;
        }

        @Override
        public boolean switchOff(Object actionData) {
            active = false;
            this.actionData = actionData;

            long duration = (switchOnTime == 0) ? 0 : System.currentTimeMillis() - switchOnTime;
            actions.add(new ActionItem(this, "off", (int) duration));

            switchOnTime = 0;
            return !broken;
        }

        @Override
        public boolean isOn() {
            return active;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getLabel() {
            return id;
        }

        @Override
        public boolean setValue(int val, Object actionData) {
            throw new NotImplementedException();
        }

        @Override
        public Object getLastActionData() {
            return actionData;
        }

        @Override
        public void removeActionData() {
            actionData = null;
        }

        @Override
        public int getValue() {
            throw new NotImplementedException();
        }

        @Override
        public void setActionData(Object actionData) {
            this.actionData = actionData;
        }

        @Override
        public void callListenersAndSetActionData(boolean invert, Object actionData) {
        }

        @Override
        public String toString() {
            return "Actor{" +
                    "id='" + id + '\'' +
                    '}';
        }
    }
}
