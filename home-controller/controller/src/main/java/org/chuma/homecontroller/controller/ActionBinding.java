package org.chuma.homecontroller.controller;

import org.chuma.homecontroller.base.node.NodePin;
import org.chuma.homecontroller.controller.action.Action;

/**
 * Binds actions to change of specific node pin.
 */
public class ActionBinding {
    private final NodePin trigger;
    private final Action[] buttonDownActions;
    private final Action[] buttonUpActions;

    public ActionBinding(NodePin trigger, Action buttonDownAction, Action buttonUpAction) {
        this(trigger, (buttonDownAction != null) ? new Action[]{buttonDownAction} : null,
                (buttonUpAction != null) ? new Action[]{buttonUpAction} : null);
    }

    public ActionBinding(NodePin trigger, Action[] buttonDownActions, Action[] buttonUpActions) {
        this.trigger = trigger;
        this.buttonDownActions = buttonDownActions;
        this.buttonUpActions = buttonUpActions;
    }

    /**
     * Get node pin triggering the actions.
     */
    public NodePin getTrigger() {
        return trigger;
    }

    public String toString() {
        return trigger.toString();
    }

    /**
     * Get actions called when pin does 0 (button pressed).
     */
    public Action[] getButtonDownActions() {
        return buttonDownActions;
    }

    /**
     * Get actions called when pin does 1 (button released).
     */
    public Action[] getButtonUpActions() {
        return buttonUpActions;
    }
}