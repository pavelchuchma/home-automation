package org.chuma.homecontroller.controller;

import java.util.Date;

import org.chuma.homecontroller.controller.action.Action;
import org.chuma.homecontroller.controller.actor.Actor;

public class PirStatus {
    String id;
    String name;
    Date lastActivate;
    boolean active;
    Actor actor = new ActorImpl();

    public PirStatus(String id, String name) {
        this.id = id;
        this.name = name;
        active = false;
    }

    public String getId() {
        return id;
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
            public void perform(int timeSinceLastAction) {
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
            public void perform(int timeSinceLastAction) {
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
        public String getLabel() {
            return name;
        }

        @Override
        public Object getActionData() {
            return null;
        }

        @Override
        public void setActionData(Object actionData) {
        }

        @Override
        public void callListenersAndSetActionData(Object actionData) {
        }
    }
}