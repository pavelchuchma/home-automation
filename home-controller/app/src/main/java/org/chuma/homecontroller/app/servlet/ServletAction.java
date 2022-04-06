package org.chuma.homecontroller.app.servlet;

import org.chuma.homecontroller.controller.action.Action;

public class ServletAction {
    private final String id;
    private final String label;
    private final Action action;

    public ServletAction(String id, String label, Action action) {
        this.id = id;
        this.label = label;
        this.action = action;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public Action getAction() {
        return action;
    }
}
