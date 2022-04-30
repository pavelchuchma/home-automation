package org.chuma.homecontroller.controller;

import org.chuma.homecontroller.base.node.NodePin;
import org.chuma.homecontroller.controller.action.Action;

/**
 * Binds actions to change of specific node pin.
 */
public class ActionBinding {
    private final NodePin trigger;
    private final Action[] onInputLowActions;
    private final Action[] onInputHighActions;

    public ActionBinding(NodePin trigger, Action onInputLowAction, Action onInputHighAction) {
        this(trigger, (onInputLowAction != null) ? new Action[]{onInputLowAction} : null,
                (onInputHighAction != null) ? new Action[]{onInputHighAction} : null);
    }

    public ActionBinding(NodePin trigger, Action[] onInputLowActions, Action[] onInputHighActions) {
        this.trigger = trigger;
        this.onInputLowActions = onInputLowActions;
        this.onInputHighActions = onInputHighActions;
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
     * Get actions called when pin does 0.
     */
    public Action[] getOnInputLowActions() {
        return onInputLowActions;
    }

    /**
     * Get actions called when pin does 1.
     */
    public Action[] getOnInputHighActions() {
        return onInputHighActions;
    }
}