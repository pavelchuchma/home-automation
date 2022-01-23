package org.chuma.homecontroller.app.servlet;

import org.chuma.homecontroller.controller.action.Action;

public class ServletAction {
    final public String name;
    final public Action action;

    public ServletAction(String name, Action action) {
        this.name = name;
        this.action = action;
    }
}
