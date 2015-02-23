package controller;

import controller.Action.Action;
import controller.actor.Actor;

import java.util.Date;

public class PirStatus {
    String name;
    Date lastActivate;
    boolean active;
    Actor actor = new ActorImpl();

    public PirStatus(String name) {
        this.name = name;
        active = false;
    }

    public String getName() {
        return name;
    }

    public Date getLastActivate() {
        return lastActivate;
    }

    public boolean isActive() {
        return active;
    }

    public Action getActivateAction() {
        return new Action() {
            @Override
            public void perform(int previousDurationMs) {
                active = true;
                lastActivate = new Date();
            }

            @Override
            public Actor getActor() {
                return actor;
            }
        };
    }
    public Action getDeactivateAction() {
        return new Action() {
            @Override
            public void perform(int previousDurationMs) {
                active = false;
            }

            @Override
            public Actor getActor() {
                return actor;
            }
        };
    }

    private class ActorImpl implements Actor {

        @Override
        public String getId() {
            return name;
        }

        @Override
        public boolean setValue(int val, Object actionData) {
            return true;
        }

        @Override
        public Object getLastActionData() {
            return null;
        }

        @Override
        public void removeActionData() {
        }

        @Override
        public int getValue() {
            return 0;
        }

        @Override
        public void setActionData(Object actionData) {
        }

        @Override
        public void setIndicatorsAndActionData(boolean invert, Object actionData) {
        }
    }
}