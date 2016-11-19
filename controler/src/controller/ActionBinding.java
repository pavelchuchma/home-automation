package controller;

import controller.action.Action;
import node.NodePin;

public class ActionBinding {
    NodePin trigger;

    Action[] buttonDownActions;
    Action[] buttonUpActions;

    public ActionBinding(NodePin trigger, Action buttonDownAction, Action buttonUpAction) {
        this(trigger, (buttonDownAction != null) ? new Action[]{buttonDownAction} : null,
                (buttonUpAction != null) ? new Action[]{buttonUpAction} : null);
    }

    public ActionBinding(NodePin trigger, Action[] buttonDownActions, Action[] buttonUpActions) {
        this.trigger = trigger;
        this.buttonDownActions = buttonDownActions;
        this.buttonUpActions = buttonUpActions;
    }

    public NodePin getTrigger() {
        return trigger;
    }

    public String toString() {
        return trigger.toString();
    }

    public Action[] getButtonDownActions() {
        return buttonDownActions;
    }

    public Action[] getButtonUpActions() {
        return buttonUpActions;
    }
}