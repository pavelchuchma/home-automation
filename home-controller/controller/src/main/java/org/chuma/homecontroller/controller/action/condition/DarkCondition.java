package org.chuma.homecontroller.controller.action.condition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.chuma.homecontroller.controller.actor.IOnOffActor;
import org.chuma.homecontroller.controller.actor.IReadableOnOff;

import java.util.Arrays;

/**
 * Is true if all registered actors are off
 */
public class DarkCondition implements ICondition {
    static Logger log = LoggerFactory.getLogger(DarkCondition.class.getName());
    ICondition precondition;
    IOnOffActor[] actors;

    public DarkCondition(ICondition precondition, IOnOffActor[] actors) {
        this.precondition = precondition;
        this.actors = actors;
    }

    @Override
    public boolean isTrue(int previousDurationMs) {
        return precondition.isTrue(previousDurationMs) && Arrays.stream(actors).noneMatch(IReadableOnOff::isOn);
    }
}
