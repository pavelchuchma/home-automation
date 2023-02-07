package org.chuma.homecontroller.controller.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.chuma.homecontroller.controller.actor.IOnOffActor;

public class AbstractControllerTest {
    List<ActionItem> actions = new ArrayList<>();

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

            ActionItem that = (ActionItem)o;

            if (Math.abs(value - that.value) > tolerance) return false;
            if (!Objects.equals(actionName, that.actionName)) return false;
            return actor == that.actor;
        }

        @Override
        public String toString() {
            return String.format("ActionItem{actor=%s, actionName='%s', value=%d(%d)}", actor, actionName, value, tolerance);
        }
    }

    public class Actor implements IOnOffActor {
        private final String id;
        boolean active = false;
        Object actionData;
        long switchOnTime;
        boolean broken = false;

        public Actor(String id) {
            this.id = id;
        }

        public void breakIt() {
            broken = true;
        }

        @Override
        public boolean switchOn(Object actionData) {
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
            actions.add(new ActionItem(this, "off", (int)duration));

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
        public Object getActionData() {
            return actionData;
        }

        @Override
        public void setActionData(Object actionData) {
            this.actionData = actionData;
        }

        @Override
        public void callListenersAndSetActionData(Object actionData) {
        }

        @Override
        public String toString() {
            return "Actor{" +
                    "id='" + id + '\'' +
                    '}';
        }
    }
}
