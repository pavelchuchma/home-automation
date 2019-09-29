package controller.action.condition;

import java.util.Arrays;

import controller.actor.IOnOffActor;
import controller.actor.IReadableOnOff;
import org.apache.log4j.Logger;

public class DarkCondition implements ICondition {
    static Logger log = Logger.getLogger(DarkCondition.class.getName());
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
