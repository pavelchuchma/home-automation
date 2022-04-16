package org.chuma.homecontroller.app.train;

import java.util.function.IntConsumer;

import org.chuma.homecontroller.base.node.NodePin;
import org.chuma.homecontroller.controller.ActionBinding;
import org.chuma.homecontroller.controller.action.Action;
import org.chuma.homecontroller.controller.actor.Actor;

/**
 * Simplified implementation of {@link ActionBinding} for up/down actions passed as lambdas.
 * The actions receive previousDurationMs as parameter (see {@link Action#perform(int)}.
 */
public class SimpleActionBinding extends ActionBinding {
    public SimpleActionBinding(NodePin trigger, IntConsumer buttonDownAction, IntConsumer buttonUpAction) {
        super(trigger, action(buttonDownAction), action(buttonUpAction));
    }

    private static Action action(IntConsumer c) {
        return new Action() {
            @Override
            public void perform(int previousDurationMs) {
                c.accept(previousDurationMs);
            }

            @Override
            public Actor getActor() {
                return null;
            }
        };
    }
}
