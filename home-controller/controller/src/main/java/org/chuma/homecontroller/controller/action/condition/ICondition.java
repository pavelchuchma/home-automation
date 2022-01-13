package org.chuma.homecontroller.controller.action.condition;

public interface ICondition {
    boolean isTrue(int previousDurationMs);
}
